/*
 * SPDX-FileCopyrightText: 2021 SohnyBohny <sohny.bean@streber24.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.MouseReceiverPlugin

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.cosmicext.connect.R
import java.util.*

class MouseReceiverService : AccessibilityService() {

    private var cursorView: View? = null
    private var cursorLayout: LayoutParams? = null
    private var windowManager: WindowManager? = null
    private var runHandler: Handler? = null
    private val hideRunnable = Runnable {
        cursorView?.visibility = View.GONE
        Log.i("MouseReceiverService", "Hiding pointer due to inactivity")
    }
    private var swipeStoke: GestureDescription.StrokeDescription? = null
    private var scrollSum = 0.0

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i("MouseReceiverService", "created")
    }

    override fun onServiceConnected() {
        // Create an overlay and display the cursor
        windowManager = ContextCompat.getSystemService(this, WindowManager::class.java)
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        cursorView = View.inflate(baseContext, R.layout.mouse_receiver_cursor, null)
        cursorLayout = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            LayoutParams.FLAG_DISMISS_KEYGUARD or LayoutParams.FLAG_NOT_FOCUSABLE
                    or LayoutParams.FLAG_NOT_TOUCHABLE or LayoutParams.FLAG_FULLSCREEN
                    or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // allow cursor to move over status bar on devices having a display cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cursorLayout?.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        cursorLayout?.apply {
            gravity = Gravity.START or Gravity.TOP
            x = displayMetrics.widthPixels / 2
            y = displayMetrics.heightPixels / 2
        }

        cursorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        windowManager?.addView(cursorView, cursorLayout)

        runHandler = Handler(Looper.getMainLooper())

        cursorView?.visibility = View.GONE
    }

    private fun hideAfter5Seconds() {
        runHandler?.removeCallbacks(hideRunnable)
        runHandler?.postDelayed(hideRunnable, 5000)
    }

    fun getX(): Float {
        return (cursorLayout?.x ?: 0) + (cursorView?.width ?: 0) / 2f
    }

    fun getY(): Float {
        return (cursorLayout?.y ?: 0) + (cursorView?.height ?: 0) / 2f
    }

    fun moveView(dx: Double, dy: Double) {
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getRealMetrics(displayMetrics)

        cursorLayout?.apply {
            x += dx.toInt()
            y += dy.toInt()
        }

        if (getX() > displayMetrics.widthPixels)
            cursorLayout?.x = displayMetrics.widthPixels - (cursorView?.width ?: 0) / 2
        if (getY() > displayMetrics.heightPixels)
            cursorLayout?.y = displayMetrics.heightPixels - (cursorView?.height ?: 0) / 2
        if (getX() < 0) cursorLayout?.x = -(cursorView?.width ?: 0) / 2
        if (getY() < 0) cursorLayout?.y = -(cursorView?.height ?: 0) / 2

        Handler(mainLooper).post {
            try {
                windowManager?.updateViewLayout(cursorView, cursorLayout)
                cursorView?.visibility = View.VISIBLE
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    private fun isSwiping(): Boolean = swipeStoke != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startSwipe(): Boolean {
        val path = Path().apply {
            moveTo(getX(), getY())
        }
        swipeStoke = GestureDescription.StrokeDescription(path, 0, 1, true)
        val builder = GestureDescription.Builder()
        builder.addStroke(swipeStoke!!)
        (cursorView?.findViewById<ImageView>(R.id.mouse_cursor))?.setImageResource(R.drawable.mouse_pointer_clicked)
        return dispatchGesture(builder.build(), null, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun continueSwipe(fromX: Float, fromY: Float): Boolean {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(getX(), getY())
        }
        swipeStoke = swipeStoke?.continueStroke(path, 0, 5, true)
        val builder = GestureDescription.Builder()
        swipeStoke?.let { builder.addStroke(it) }
        return dispatchGesture(builder.build(), null, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopSwipe(): Boolean {
        val path = Path().apply {
            moveTo(getX(), getY())
        }
        if (swipeStoke == null) return true
        
        swipeStoke = swipeStoke?.continueStroke(path, 0, 1, false)
        val builder = GestureDescription.Builder()
        swipeStoke?.let { builder.addStroke(it) }
        swipeStoke = null
        (cursorView?.findViewById<ImageView>(R.id.mouse_cursor))?.setImageResource(R.drawable.mouse_pointer)
        return dispatchGesture(builder.build(), null, null)
    }

    private fun findNodeByAction(root: AccessibilityNodeInfo?, action: AccessibilityNodeInfo.AccessibilityAction): AccessibilityNodeInfo? {
        if (root == null) return null
        val deque: Deque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node = deque.removeFirst()
            if (node.actionList.contains(action)) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { deque.addLast(it) }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (windowManager != null && cursorView != null) {
            windowManager?.removeView(cursorView)
        }
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    companion object {
        @JvmStatic
        var instance: MouseReceiverService? = null
            private set

        @JvmStatic
        fun move(dx: Double, dy: Double): Boolean {
            val current = instance ?: return false
            val fromX = current.getX()
            val fromY = current.getY()

            current.moveView(dx, dy)
            current.hideAfter5Seconds()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && current.isSwiping()) {
                return current.continueSwipe(fromX, fromY)
            }
            return true
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun createClick(x: Float, y: Float, duration: Int): GestureDescription {
            val clickPath = Path().apply { moveTo(x, y) }
            val clickStroke = GestureDescription.StrokeDescription(clickPath, 0, duration.toLong())
            return GestureDescription.Builder().addStroke(clickStroke).build()
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun click(): Boolean {
            val current = instance ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && current.isSwiping()) {
                return current.stopSwipe()
            }
            return click(current.getX(), current.getY())
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun click(x: Float, y: Float): Boolean {
            val current = instance ?: return false
            return current.dispatchGesture(createClick(x, y, 1), null, null)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun longClick(): Boolean {
            val current = instance ?: return false
            return current.dispatchGesture(
                createClick(current.getX(), current.getY(), ViewConfiguration.getLongPressTimeout()),
                null,
                null
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        fun longClickSwipe(): Boolean {
            val current = instance ?: return false
            return if (current.isSwiping()) current.stopSwipe() else current.startSwipe()
        }

        @JvmStatic
        fun scroll(dx: Double, dy: Double): Boolean {
            val current = instance ?: return false
            current.scrollSum += dy
            if (Math.signum(dy) != Math.signum(current.scrollSum)) current.scrollSum = dy
            if (Math.abs(current.scrollSum) < 500) return false
            current.scrollSum = 0.0

            val action = if (dy > 0) AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD
            else AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD

            val scrollable = current.findNodeByAction(current.rootInActiveWindow, action) ?: return false

            return scrollable.performAction(action.id)
        }

        @JvmStatic
        fun backButton(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false

        @JvmStatic
        fun homeButton(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false

        @JvmStatic
        fun recentButton(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false

        @JvmStatic
        fun powerButton(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
    }
}
