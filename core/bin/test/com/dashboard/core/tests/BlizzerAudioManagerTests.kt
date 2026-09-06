package com.dashboard.core.tests

import com.dashboard.core.hardware.AudioEvent
import com.dashboard.core.hardware.mock.MockAudioOutput
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.BlizzerAudioManager
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals

fun blizzerAudioManagerSuite() = TestSuite("BlizzerAudioManager").apply {

    test("beeps once when a camera warning first appears") {
        val phone = MockPhoneCommunication()
        val blizzerManager = BlizzerManager(phone)
        val audio = MockAudioOutput()
        val audioManager = BlizzerAudioManager(blizzerManager, audio)
        blizzerManager.start()
        audioManager.start()

        phone.triggerCameraWarning(500)

        assertEquals(1, audio.playedEvents.count { it == AudioEvent.BLIZZER_ALERT }, "should beep once for the first threshold")
    }

    test("beeps again for each new (closer) distance threshold on the same event") {
        val phone = MockPhoneCommunication()
        val blizzerManager = BlizzerManager(phone)
        val audio = MockAudioOutput()
        val audioManager = BlizzerAudioManager(blizzerManager, audio)
        blizzerManager.start()
        audioManager.start()

        phone.triggerCameraWarning(500)
        phone.triggerCameraWarning(200)
        phone.triggerCameraWarning(100)

        assertEquals(3, audio.playedEvents.count { it == AudioEvent.BLIZZER_ALERT }, "should beep once per distinct threshold")
    }

    test("does not beep again for a repeated identical distance") {
        val phone = MockPhoneCommunication()
        val blizzerManager = BlizzerManager(phone)
        val audio = MockAudioOutput()
        val audioManager = BlizzerAudioManager(blizzerManager, audio)
        blizzerManager.start()
        audioManager.start()

        phone.triggerCameraWarning(500)
        phone.triggerCameraWarning(500) // same threshold again — should not re-beep

        assertEquals(1, audio.playedEvents.count { it == AudioEvent.BLIZZER_ALERT }, "should not beep twice for the same threshold")
    }

    test("dismissing and triggering a fresh event beeps again") {
        val phone = MockPhoneCommunication()
        val blizzerManager = BlizzerManager(phone)
        val audio = MockAudioOutput()
        val audioManager = BlizzerAudioManager(blizzerManager, audio)
        blizzerManager.start()
        audioManager.start()

        val id = phone.triggerCameraWarning(500)
        phone.dismissBlizzer(id)
        phone.triggerCameraWarning(500) // a fresh camera approach

        assertEquals(2, audio.playedEvents.count { it == AudioEvent.BLIZZER_ALERT }, "a new event after dismissal should beep again")
    }
}
