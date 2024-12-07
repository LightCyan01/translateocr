package com.jaymie.translateocr.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// https://developer.android.com/guide/topics/ui/accessibility/service
class TranslateAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    
    companion object {
        private val _screenText = MutableLiveData<List<TextBlock>>()
        val screenText: LiveData<List<TextBlock>> = _screenText
        
        private var instance: TranslateAccessibilityService? = null
        
        fun getInstance(): TranslateAccessibilityService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    serviceScope.launch {
                        processScreenContent()
                    }
                }
                else -> { /* ignore other event types */ }
            }
        }
    }

    private inline fun <R> AccessibilityNodeInfo.safeUse(block: (AccessibilityNodeInfo) -> R): R {
        try {
            return block(this)
        } finally {
            @Suppress("DEPRECATION")
            this.recycle()
        }
    }

    private fun processScreenContent() {
        rootInActiveWindow?.safeUse { rootNode ->
            val textBlocks = mutableListOf<TextBlock>()
            findTextNodes(rootNode, textBlocks)
            _screenText.postValue(textBlocks)
        }
    }

    private fun findTextNodes(node: AccessibilityNodeInfo, textBlocks: MutableList<TextBlock>) {
        if (!node.text.isNullOrBlank()) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            textBlocks.add(TextBlock(
                text = node.text.toString(),
                bounds = bounds,
                isEditable = node.isEditable
            ))
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.safeUse { child ->
                findTextNodes(child, textBlocks)
            }
        }
    }

    override fun onInterrupt() {
        // override
    }

    data class TextBlock(
        val text: String,
        val bounds: Rect,
        val isEditable: Boolean
    )
} 