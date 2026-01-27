package org.cosmic.cconnect.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Compose Test Utilities
 *
 * Utilities for testing Jetpack Compose UI.
 * Provides common patterns and helpers for Compose screen testing.
 */
object ComposeTestUtils {

  /**
   * Wait for a composable to appear.
   *
   * @param testRule Compose test rule
   * @param text Text to find
   * @param timeoutMs Timeout in milliseconds
   * @return true if found, false if timeout
   */
  fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = 5000): Boolean {
    return try {
      waitUntil(timeoutMillis = timeoutMs) {
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Wait for a composable with content description to appear.
   */
  fun ComposeTestRule.waitForContentDescription(
    description: String,
    timeoutMs: Long = 5000
  ): Boolean {
    return try {
      waitUntil(timeoutMillis = timeoutMs) {
        onAllNodesWithContentDescription(description, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Wait for a dialog to appear.
   */
  fun ComposeTestRule.waitForDialog(timeoutMs: Long = 5000): Boolean {
    return try {
      waitUntil(timeoutMillis = timeoutMs) {
        // Dialogs typically have a scrim/overlay
        onAllNodesWithTag("dialog").fetchSemanticsNodes().isNotEmpty() ||
        onAllNodes(hasTestTag("dialog")).fetchSemanticsNodes().isNotEmpty()
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Assert text exists and is displayed.
   */
  fun ComposeTestRule.assertTextDisplayed(text: String) {
    onNodeWithText(text, substring = true)
      .assertExists()
      .assertIsDisplayed()
  }

  /**
   * Assert text does not exist.
   */
  fun ComposeTestRule.assertTextNotDisplayed(text: String) {
    onNodeWithText(text, substring = true).assertDoesNotExist()
  }

  /**
   * Assert button is enabled.
   */
  fun ComposeTestRule.assertButtonEnabled(text: String) {
    onNodeWithText(text)
      .assertExists()
      .assertIsEnabled()
  }

  /**
   * Assert button is disabled.
   */
  fun ComposeTestRule.assertButtonDisabled(text: String) {
    onNodeWithText(text)
      .assertExists()
      .assertIsNotEnabled()
  }

  /**
   * Click a button by text.
   */
  fun ComposeTestRule.clickButton(text: String) {
    onNodeWithText(text)
      .assertExists()
      .assertHasClickAction()
      .performClick()
  }

  /**
   * Click an item in a list by text.
   */
  fun ComposeTestRule.clickListItem(text: String) {
    onNodeWithText(text, substring = true)
      .assertExists()
      .performClick()
  }

  /**
   * Enter text into a text field.
   */
  fun ComposeTestRule.enterText(label: String, text: String) {
    onNodeWithText(label)
      .performTextInput(text)
  }

  /**
   * Clear and enter text.
   */
  fun ComposeTestRule.clearAndEnterText(label: String, text: String) {
    onNodeWithText(label)
      .performTextClearance()
      .performTextInput(text)
  }

  /**
   * Assert toggle is checked.
   */
  fun ComposeTestRule.assertToggleOn(label: String) {
    onNode(
      hasText(label) and hasSetTextAction().not()
    ).assertIsOn()
  }

  /**
   * Assert toggle is not checked.
   */
  fun ComposeTestRule.assertToggleOff(label: String) {
    onNode(
      hasText(label) and hasSetTextAction().not()
    ).assertIsOff()
  }

  /**
   * Toggle a switch.
   */
  fun ComposeTestRule.toggleSwitch(label: String) {
    onNode(
      hasText(label) and hasSetTextAction().not()
    ).performClick()
  }

  /**
   * Scroll to item in lazy list.
   */
  fun ComposeTestRule.scrollToItem(text: String) {
    onNodeWithText(text, substring = true)
      .performScrollTo()
  }

  /**
   * Assert loading indicator is shown.
   */
  fun ComposeTestRule.assertLoading() {
    onNode(
      hasContentDescription("Loading") or
      hasText("Loading", substring = true)
    ).assertExists()
  }

  /**
   * Assert error is shown.
   */
  fun ComposeTestRule.assertError(errorMessage: String? = null) {
    if (errorMessage != null) {
      onNodeWithText(errorMessage, substring = true).assertExists()
    } else {
      onNode(
        hasText("Error", substring = true) or
        hasContentDescription("Error")
      ).assertExists()
    }
  }

  /**
   * Assert empty state is shown.
   */
  fun ComposeTestRule.assertEmptyState(message: String? = null) {
    if (message != null) {
      onNodeWithText(message, substring = true).assertExists()
    } else {
      onNode(
        hasText("No", substring = true) or
        hasText("Empty", substring = true)
      ).assertExists()
    }
  }

  /**
   * Dismiss dialog by clicking outside.
   */
  fun ComposeTestRule.dismissDialog() {
    // Click on scrim/overlay to dismiss
    onNode(isDialog()).performClick()
  }

  /**
   * Confirm dialog action.
   */
  fun ComposeTestRule.confirmDialog(confirmText: String = "Confirm") {
    clickButton(confirmText)
  }

  /**
   * Cancel dialog action.
   */
  fun ComposeTestRule.cancelDialog(cancelText: String = "Cancel") {
    clickButton(cancelText)
  }
}
