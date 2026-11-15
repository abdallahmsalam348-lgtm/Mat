package com.matchly.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

enum class ErrorKind {
    OFFLINE,
    TIMEOUT,
    RATE_LIMITED,
    NOT_CONFIGURED,
    UNAUTHORIZED,
    PLAN_LIMIT,
    NOT_FOUND,
    SERVER,
    UNKNOWN;

    companion object {
        fun from(t: Throwable): ErrorKind = when (t) {
            is ApiException -> t.kind
            is SocketTimeoutException -> TIMEOUT
            is HttpException -> fromHttp(t.code(), bodyOf(t))
            is SerializationException -> SERVER
            is IOException -> OFFLINE
            else -> UNKNOWN
        }

        private fun bodyOf(e: HttpException): String =
            try {
                e.response()?.errorBody()?.string().orEmpty()
            } catch (x: Exception) {
                ""
            }

        fun fromHttp(code: Int, body: String): ErrorKind = when {
            body.contains("provider_not_configured") -> NOT_CONFIGURED
            body.contains("plan_restricted") -> PLAN_LIMIT
            code == 429 -> RATE_LIMITED
            code == 401 || code == 403 -> UNAUTHORIZED
            code == 404 -> NOT_FOUND
            code in 500..599 -> SERVER
            else -> UNKNOWN
        }
    }
}

class ApiException(val kind: ErrorKind, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: kind.name, cause)

/** Where a piece of data came from, so the UI can be honest about it. */
data class SourceInfo(
    val demo: Boolean = false,
    val fromCache: Boolean = false,
    val savedAt: Long? = null,
    val error: ErrorKind? = null,
    val refreshing: Boolean = false,
)

data class Loaded<T>(val data: T, val info: SourceInfo)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Failure(val kind: ErrorKind) : UiState<Nothing>
    data class Content<out T>(val data: T, val info: SourceInfo) : UiState<T>
}

/** Runs a loading flow in a scope and exposes it as [UiState]. Cancels the previous run on reload. */
class Loader<T>(
    private val scope: CoroutineScope,
    private val source: (force: Boolean) -> Flow<Loaded<T>>,
) {
    private val mutable = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = mutable
    private var job: Job? = null

    fun load(force: Boolean = false) {
        job?.cancel()
        job = scope.launch {
            val current = mutable.value
            if (current is UiState.Content<T>) {
                mutable.value = UiState.Content(current.data, current.info.copy(refreshing = true, error = null))
            } else {
                mutable.value = UiState.Loading
            }
            try {
                source(force).collect { mutable.value = UiState.Content(it.data, it.info) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val kind = ErrorKind.from(e)
                val cur = mutable.value
                mutable.value = if (cur is UiState.Content<T>) {
                    UiState.Content(cur.data, cur.info.copy(refreshing = false, error = kind))
                } else {
                    UiState.Failure(kind)
                }
            }
        }
    }
}
