package com.example.aiskipads

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdSkipService : AccessibilityService() {

    private val adScanner = ADScanner()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AdSkipService", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 2. Check event type
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            
            // 3. Recursive traversal to extract text and bounds
            val textList = mutableListOf<String>()
            val boundsMap = mutableMapOf<String, Rect>()
            collectText(rootNode, textList, boundsMap)
            
            // 4. Get coordinates from scanner
            val coordinate = adScanner.detectAndClick(textList, boundsMap)
            
            // 5. Build gesture and click
            if (coordinate != null) {
                Log.d("AdSkipService", "Clicking at ${coordinate.x}, ${coordinate.y}")
                clickAt(coordinate.x, coordinate.y)
            }
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?, list: MutableList<String>, map: MutableMap<String, Rect>) {
        if (node == null) return
        
        val text = node.text?.toString()
        if (!text.isNullOrEmpty()) {
            list.add(text)
            val rect = Rect()
            node.getBoundsInScreen(rect)
            map[text] = rect
        }
        
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), list, map)
        }
    }

    private fun clickAt(x: Int, y: Int) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(builder.build(), null, null)
    }

    override fun onInterrupt() {
        Log.d("AdSkipService", "Service Interrupted")
    }
}
