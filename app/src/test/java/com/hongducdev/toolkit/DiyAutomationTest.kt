package com.hongducdev.toolkit

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hongducdev.toolkit.domain.diy.Action
import com.hongducdev.toolkit.domain.diy.ActionGsonAdapter
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.DiyState
import com.hongducdev.toolkit.domain.diy.SealedAdapter
import com.hongducdev.toolkit.domain.diy.Trigger
import com.hongducdev.toolkit.services.automation.modules.TimeModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DiyAutomationTest {
    private val gson =
        GsonBuilder()
            .registerTypeAdapter(Trigger::class.java, SealedAdapter(Trigger::class))
            .registerTypeAdapter(DiyState::class.java, SealedAdapter(DiyState::class))
            .registerTypeAdapter(Action::class.java, SealedAdapter(Action::class))
            .create()

    @Test
    fun testTriggerAutomationSerialization() {
        val rule =
            Automation(
                id = "test-trigger-1",
                name = "Cắm sạc bật đèn pin",
                type = Automation.Type.TRIGGER,
                trigger = Trigger.ChargerConnected,
                actions =
                    listOf(
                        Action.TurnOnFlashlight,
                        Action.Wait(seconds = 5),
                        Action.SendNotification(notifTitle = "Đã cắm sạc", notifMessage = "Đang sạc thiết bị"),
                        Action.HapticVibration,
                    ),
                isEnabled = true,
            )

        val json = gson.toJson(rule)
        assertTrue(json.contains("\"type\":\"TRIGGER\""))
        assertTrue(json.contains("\"type\":\"ChargerConnected\""))
        assertTrue(json.contains("\"type\":\"TurnOnFlashlight\""))
        assertTrue(json.contains("\"seconds\":5"))

        val deserialized = gson.fromJson(json, Automation::class.java)
        assertEquals("test-trigger-1", deserialized.id)
        assertEquals("Cắm sạc bật đèn pin", deserialized.name)
        assertEquals(Automation.Type.TRIGGER, deserialized.type)
        assertTrue(deserialized.trigger is Trigger.ChargerConnected)
        assertEquals(4, deserialized.actionList.size)
        assertTrue(deserialized.actionList[0] is Action.TurnOnFlashlight)
        assertTrue(deserialized.actionList[1] is Action.Wait)
        assertEquals(5, (deserialized.actionList[1] as Action.Wait).seconds)
        assertTrue(deserialized.actionList[2] is Action.SendNotification)
        assertEquals("Đã cắm sạc", (deserialized.actionList[2] as Action.SendNotification).notifTitle)
    }

    @Test
    fun testStateAutomationSerialization() {
        val rule =
            Automation(
                id = "test-state-1",
                name = "Trạng thái pin yếu",
                type = Automation.Type.STATE,
                state = DiyState.BatteryLevel(minLevel = 5, maxLevel = 15),
                entryActions = listOf(Action.DimWallpaper(dimAmount = 0.8f)),
                exitActions = listOf(Action.DimWallpaper(dimAmount = 0.0f)),
                isEnabled = true,
            )

        val json = gson.toJson(rule)
        assertTrue(json.contains("\"type\":\"STATE\""))
        assertTrue(json.contains("\"type\":\"BatteryLevel\""))
        assertTrue(json.contains("\"minLevel\":5"))
        assertTrue(json.contains("\"maxLevel\":15"))

        val deserialized = gson.fromJson(json, Automation::class.java)
        assertEquals("test-state-1", deserialized.id)
        assertEquals(Automation.Type.STATE, deserialized.type)
        assertTrue(deserialized.state is DiyState.BatteryLevel)
        val batteryState = deserialized.state as DiyState.BatteryLevel
        assertEquals(5, batteryState.minLevel)
        assertEquals(15, batteryState.maxLevel)
        assertEquals(1, deserialized.entryActionList.size)
        assertEquals(1, deserialized.exitActionList.size)
    }

    @Test
    fun testActionGsonAdapterList() {
        val actions =
            listOf(
                Action.TurnOnWifi,
                Action.TurnOffWifi,
                Action.ToggleWifi,
                Action.SetVolume(streamType = 3, volumePercent = 80),
                Action.ToggleLunarLockscreen,
                Action.OpenCarDashboard,
            )

        val json = ActionGsonAdapter.listToJson(actions)
        val deserialized = ActionGsonAdapter.listFromJson(json)

        assertEquals(6, deserialized.size)
        assertTrue(deserialized[0] is Action.TurnOnWifi)
        assertTrue(deserialized[1] is Action.TurnOffWifi)
        assertTrue(deserialized[2] is Action.ToggleWifi)
        assertTrue(deserialized[3] is Action.SetVolume)
        assertEquals(80, (deserialized[3] as Action.SetVolume).volumePercent)
        assertTrue(deserialized[4] is Action.ToggleLunarLockscreen)
        assertTrue(deserialized[5] is Action.OpenCarDashboard)
    }

    @Test
    fun testCalculateNextOccurrence() {
        val next = TimeModule.calculateNextOccurrence(hour = 7, minute = 30, days = emptySet())
        assertNotNull(next)
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, next.get(Calendar.MINUTE))
        assertEquals(0, next.get(Calendar.SECOND))
    }
}
