package com.jaymie.translateocr.utils
 
/**
 * Used as a wrapper for data that is exposed via LiveData that represents an event.
 * Prevents events from being handled multiple times when observers are re-subscribed.
 *
 * Example usage:
 * ```
 * private val _showDialog = MutableLiveData<Event<String>>()
 * val showDialog: LiveData<Event<String>> = _showDialog
 *
 * // Trigger the event
 * _showDialog.value = Event("Show this message")
 *
 * // Observe the event
 * showDialog.observe(viewLifecycleOwner) { event ->
 *     event.getContentIfNotHandled()?.let { message ->
 *         // Handle the message only once
 *         showDialog(message)
 *     }
 * }
 * ```
 */
open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     * @return The content if it hasn't been handled, null otherwise
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

}