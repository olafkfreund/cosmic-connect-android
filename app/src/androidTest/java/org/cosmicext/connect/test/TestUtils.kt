package org.cosmicext.connect.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.cosmicext.connect.Device
import org.cosmicext.connect.NetworkPacket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test Utilities
 *
 * Common utilities for integration and UI tests.
 */
object TestUtils {

  /**
   * Get test application context.
   */
  fun getTestContext(): Context {
    return ApplicationProvider.getApplicationContext()
  }

  /**
   * Wait for a condition with timeout.
   *
   * @param timeoutMs Timeout in milliseconds
   * @param condition Condition to wait for
   * @return true if condition met, false if timeout
   */
  fun waitFor(timeoutMs: Long = 5000, condition: () -> Boolean): Boolean {
    val endTime = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < endTime) {
      if (condition()) {
        return true
      }
      Thread.sleep(100)
    }
    return false
  }

  /**
   * Wait for a latch with timeout.
   */
  fun waitForLatch(latch: CountDownLatch, timeoutMs: Long = 5000): Boolean {
    return latch.await(timeoutMs, TimeUnit.MILLISECONDS)
  }

  /**
   * Run on UI thread and wait.
   */
  fun <T> runOnUiThreadBlocking(block: () -> T): T {
    var result: T? = null
    var exception: Throwable? = null
    val latch = CountDownLatch(1)

    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
      try {
        result = block()
      } catch (e: Throwable) {
        exception = e
      } finally {
        latch.countDown()
      }
    }

    latch.await(5, TimeUnit.SECONDS)
    exception?.let { throw it }
    return result!!
  }

  /**
   * Clean up test data.
   */
  fun cleanupTestData() {
    val context = getTestContext()

    // Clear preferences
    val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    // Clear files
    context.filesDir.listFiles()?.forEach { file ->
      if (file.name.startsWith("test_")) {
        file.delete()
      }
    }
  }

  /**
   * Generate random device ID for tests.
   */
  fun randomDeviceId(): String {
    return "test_device_${System.currentTimeMillis()}_${(0..9999).random()}"
  }

  /**
   * Generate random test name.
   */
  fun randomTestName(): String {
    return "TestDevice_${(0..9999).random()}"
  }
}
