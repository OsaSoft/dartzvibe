package cloud.osasoft.dartzvibe.ui.screen

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Launches [block] on [screenModelScope] so that it runs to the end even when the ScreenModel is disposed first.
 *
 * For writes a screen asks for just before it leaves (Abandon then pop, a settings toggle then Back): popping a screen
 * disposes its ScreenModel and cancels its scope, which would drop a write that hasn't started or finished yet.
 * [CoroutineStart.ATOMIC] starts the coroutine even if the scope is already cancelled, and [NonCancellable] keeps it
 * running past the cancellation.
 */
fun ScreenModel.launchToCompletion(block: suspend CoroutineScope.() -> Unit): Job =
    screenModelScope.launch(start = CoroutineStart.ATOMIC) { withContext(NonCancellable, block) }
