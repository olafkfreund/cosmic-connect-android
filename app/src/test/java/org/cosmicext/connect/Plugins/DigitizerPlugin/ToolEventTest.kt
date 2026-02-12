package org.cosmicext.connect.Plugins.DigitizerPlugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test ToolEvent data class used in DigitizerPlugin for stylus/pen input events.
 * Tests data class behavior, Tool enum, and serialization properties.
 */
@RunWith(RobolectricTestRunner::class)
class ToolEventTest {

    @Test
    fun `default constructor has all null fields`() {
        val event = ToolEvent()

        assertNull(event.active)
        assertNull(event.touching)
        assertNull(event.tool)
        assertNull(event.x)
        assertNull(event.y)
        assertNull(event.pressure)
    }

    @Test
    fun `full constructor with all fields set`() {
        val event = ToolEvent(
            active = true,
            touching = false,
            tool = ToolEvent.Tool.Pen,
            x = 100,
            y = 200,
            pressure = 0.75
        )

        assertTrue(event.active!!)
        assertFalse(event.touching!!)
        assertEquals(ToolEvent.Tool.Pen, event.tool)
        assertEquals(100, event.x)
        assertEquals(200, event.y)
        assertEquals(0.75, event.pressure!!, 0.001)
    }

    @Test
    fun `Tool enum has exactly Pen and Rubber values`() {
        val tools = ToolEvent.Tool.values()

        assertEquals(2, tools.size)
        assertEquals(ToolEvent.Tool.Pen, tools[0])
        assertEquals(ToolEvent.Tool.Rubber, tools[1])
    }

    @Test
    fun `copy works correctly`() {
        val original = ToolEvent(
            active = true,
            touching = true,
            tool = ToolEvent.Tool.Pen,
            x = 50,
            y = 100,
            pressure = 0.5
        )

        val copied = original.copy(x = 75, pressure = 0.8)

        assertTrue(copied.active!!)
        assertTrue(copied.touching!!)
        assertEquals(ToolEvent.Tool.Pen, copied.tool)
        assertEquals(75, copied.x) // Changed
        assertEquals(100, copied.y) // Unchanged
        assertEquals(0.8, copied.pressure!!, 0.001) // Changed
    }

    @Test
    fun `equals and hashCode work correctly`() {
        val event1 = ToolEvent(
            active = true,
            touching = false,
            tool = ToolEvent.Tool.Rubber,
            x = 150,
            y = 250,
            pressure = 0.6
        )

        val event2 = ToolEvent(
            active = true,
            touching = false,
            tool = ToolEvent.Tool.Rubber,
            x = 150,
            y = 250,
            pressure = 0.6
        )

        val event3 = ToolEvent(
            active = true,
            touching = true, // Different
            tool = ToolEvent.Tool.Rubber,
            x = 150,
            y = 250,
            pressure = 0.6
        )

        // Test equals
        assertEquals(event1, event2)
        assertNotEquals(event1, event3)

        // Test hashCode
        assertEquals(event1.hashCode(), event2.hashCode())
        assertNotEquals(event1.hashCode(), event3.hashCode())
    }

    @Test
    fun `toString includes all fields`() {
        val event = ToolEvent(
            active = true,
            touching = false,
            tool = ToolEvent.Tool.Pen,
            x = 300,
            y = 400,
            pressure = 0.9
        )

        val string = event.toString()

        assertTrue(string.contains("active=true"))
        assertTrue(string.contains("touching=false"))
        assertTrue(string.contains("tool=Pen"))
        assertTrue(string.contains("x=300"))
        assertTrue(string.contains("y=400"))
        assertTrue(string.contains("pressure=0.9"))
    }
}
