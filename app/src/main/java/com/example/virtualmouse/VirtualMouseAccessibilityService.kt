package com.example.virtualmouse

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VirtualMouseAccessibilityService : AccessibilityService() {

    private val TAG = "VirtualMouseA11y"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle events if needed
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = Intent.Filter().apply {
            addAction("com.example.virtualmouse.ACCESSIBILITY_ACTION")
        }
        registerReceiver(actionReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            val action = intent?.getIntExtra("action", -1) ?: return
            val x = intent.getIntExtra("x", 0)
            val y = intent.getIntExtra("y", 0)
            performAction(action, x, y)
        }
    }

    private fun performAction(action: Int, x: Int, y: Int) {
        try {
            val rootNode = rootInActiveWindow ?: return
            val targetNode = findNodeAt(rootNode, x, y)
            
            targetNode?.let { node ->
                when (action) {
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    }
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_COPY -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_COPY)
                    }
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_PASTE -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_PASTE)
                    }
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SELECT_ALL -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SELECT_ALL)
                    }
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    }
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                    }
                }
                node.recycle()
            }
            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing action", e)
        }
    }

    private fun findNodeAt(node: AccessibilityNodeInfo, targetX: Int, targetY: Int): AccessibilityNodeInfo? {
        try {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            if (bounds.contains(targetX, targetY)) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    val result = findNodeAt(child, targetX, targetY)
                    if (result != null) {
                        child.recycle()
                        return result
                    }
                    child.recycle()
                }
                return node
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findNodeAt", e)
        }
        return null
    }
}
