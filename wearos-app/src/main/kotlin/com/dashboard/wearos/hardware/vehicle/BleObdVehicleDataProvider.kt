package com.dashboard.wearos.hardware.vehicle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.Subscription
import com.dashboard.core.hardware.VehicleDataProvider
import com.dashboard.core.vehicle.ObdPidParser
import java.util.ArrayDeque

/**
 * Real [VehicleDataProvider] for the Car panel: a direct BLE link from the watch to an
 * ELM327-style OBD-II adapter plugged into the vehicle (NOT phone-relayed — see PLAN.md Phase 3).
 *
 * Everything vehicle-specific about the adapter lives in [ObdGattProfile]; everything about
 * decoding its replies lives in `core`'s [ObdPidParser] (unit-tested without hardware). This
 * class is only the plumbing: scan → connect → GATT discover → AT init → round-robin PID poll →
 * emit [VehicleData].
 *
 * Not exercisable in CI/emulator (needs a real dongle). `PowerManager` already calls
 * [start]/[stop] on the car waking/sleeping, so there is no lifecycle wiring to add.
 */
@SuppressLint("MissingPermission") // BLUETOOTH_SCAN/CONNECT are requested in MainActivity before start()
class BleObdVehicleDataProvider(context: Context) : VehicleDataProvider {

    private companion object {
        const val TAG = "BleObd"
        const val POLL_INTERVAL_MS = 220L
        const val RECONNECT_DELAY_MS = 4_000L
    }

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val emitter = Emitter<VehicleData>()
    private val worker = HandlerThread("BleObd").apply { start() }
    private val handler = Handler(worker.looper)

    @Volatile private var running = false
    private var gatt: BluetoothGatt? = null
    private var profile: ObdGattProfile.Candidate? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    private val commandQueue = ArrayDeque<String>()
    private var awaitingReply = false
    private val lineBuffer = StringBuilder()
    private val latestReadings = LinkedHashMap<Int, ObdPidParser.ObdReading>()
    private var pollIndex = 0
    private var initialised = false

    override fun observe(listener: (VehicleData) -> Unit): Subscription = emitter.subscribe(listener)

    override fun start() {
        if (running) return
        running = true
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "no BLE scanner (bluetooth off or unsupported)")
            return
        }
        Log.i(TAG, "scanning for an OBD adapter")
        scanner.startScan(scanCallback)
    }

    override fun stop() {
        if (!running) return
        running = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        handler.removeCallbacksAndMessages(null)
        gatt?.let { runCatching { it.disconnect() }; runCatching { it.close() } }
        gatt = null
        initialised = false
        awaitingReply = false
        commandQueue.clear()
        lineBuffer.setLength(0)
        latestReadings.clear()
    }

    // --- scanning -------------------------------------------------------------------------

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = (result.device.name ?: result.scanRecord?.deviceName ?: "").uppercase()
            val advertisesObd = result.scanRecord?.serviceUuids?.any { uuid ->
                ObdGattProfile.CANDIDATES.any { it.service == uuid.uuid }
            } == true
            if (advertisesObd || ObdGattProfile.NAME_HINTS.any { name.contains(it) }) {
                Log.i(TAG, "found adapter '${result.device.address}' ($name)")
                runCatching { adapter?.bluetoothLeScanner?.stopScan(this) }
                connect(result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed: $errorCode")
            if (running) handler.postDelayed({ if (running) start() }, RECONNECT_DELAY_MS)
        }
    }

    private fun connect(device: BluetoothDevice) {
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun scheduleReconnect() {
        if (!running) return
        gatt?.let { runCatching { it.close() } }
        gatt = null
        initialised = false
        handler.postDelayed({
            if (running) {
                Log.i(TAG, "reconnecting")
                adapter?.bluetoothLeScanner?.startScan(scanCallback)
            }
        }, RECONNECT_DELAY_MS)
    }

    // --- GATT ----------------------------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "connected, discovering services")
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "disconnected (status $status)")
                    scheduleReconnect()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) { scheduleReconnect(); return }
            val chosen = ObdGattProfile.CANDIDATES.firstNotNullOfOrNull { cand ->
                val svc = g.getService(cand.service) ?: return@firstNotNullOfOrNull null
                val w = svc.getCharacteristic(cand.write) ?: return@firstNotNullOfOrNull null
                val n = svc.getCharacteristic(cand.notify) ?: return@firstNotNullOfOrNull null
                Triple(cand, w, n)
            }
            if (chosen == null) {
                Log.w(TAG, "no known OBD GATT profile on this device")
                scheduleReconnect(); return
            }
            profile = chosen.first
            writeChar = chosen.second
            enableNotifications(g, chosen.third)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            // Notifications are on — run the AT init, then start polling.
            commandQueue.clear()
            ObdGattProfile.INIT_COMMANDS.forEach { commandQueue.add(it) }
            awaitingReply = false
            initialised = false
            sendNext()
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            onInboundBytes(value)
        }

        @Deprecated("pre-API-33 callback")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            c.value?.let { onInboundBytes(it) }
        }
    }

    private fun enableNotifications(g: BluetoothGatt, notifyChar: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(notifyChar, true)
        val cccd = notifyChar.getDescriptor(ObdGattProfile.CLIENT_CHARACTERISTIC_CONFIG) ?: run {
            // Combined char with no CCCD — just start init.
            handler.post { onDescriptorWriteFallback() }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }
    }

    private fun onDescriptorWriteFallback() {
        commandQueue.clear()
        ObdGattProfile.INIT_COMMANDS.forEach { commandQueue.add(it) }
        awaitingReply = false
        sendNext()
    }

    // --- command pump ------------------------------------------------------------------

    private fun sendNext() {
        if (awaitingReply) return
        val g = gatt ?: return
        val c = writeChar ?: return
        val cmd = commandQueue.poll() ?: run { scheduleNextPoll(); return }
        awaitingReply = true
        lineBuffer.setLength(0)
        val bytes = (cmd + "\r").toByteArray(Charsets.US_ASCII)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(c, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            run {
                c.value = bytes
                c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                g.writeCharacteristic(c)
            }
        }
        // Safety: if the adapter never answers, don't wedge the pump.
        handler.postDelayed({ if (awaitingReply) { awaitingReply = false; sendNext() } }, 1_500)
    }

    private fun onInboundBytes(value: ByteArray) {
        lineBuffer.append(String(value, Charsets.US_ASCII))
        if (lineBuffer.contains(">")) {
            val reply = lineBuffer.toString()
            lineBuffer.setLength(0)
            awaitingReply = false
            handleReply(reply)
            sendNext()
        }
    }

    private fun handleReply(reply: String) {
        if (!initialised) {
            if (commandQueue.isEmpty()) {
                initialised = true
                Log.i(TAG, "adapter initialised, polling ${ObdPidParser.POLLED_PIDS.size} PIDs")
                scheduleNextPoll()
            }
            return
        }
        ObdPidParser.parseResponse(reply).forEach { latestReadings[it.pid] = it }
        // Emit a fresh snapshot once per full round of the PID list.
        if (pollIndex == 0 && latestReadings.isNotEmpty()) {
            emitter.emit(ObdPidParser.toVehicleData(latestReadings.values.toList(), System.currentTimeMillis()))
        }
    }

    private fun scheduleNextPoll() {
        if (!running || !initialised) return
        handler.postDelayed({
            if (!running || !initialised) return@postDelayed
            val pids = ObdPidParser.POLLED_PIDS
            val pid = pids[pollIndex]
            pollIndex = (pollIndex + 1) % pids.size
            commandQueue.add(ObdGattProfile.modeOneRequest(pid))
            sendNext()
        }, POLL_INTERVAL_MS)
    }
}
