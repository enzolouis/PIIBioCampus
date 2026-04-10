package com.fneb.piibiocampus.data.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException

/**
 * Convertit toute exception Firebase/réseau en [AppException] métier.
 *
 * Usage dans un DAO :
 * ```kotlin
 * } catch (e: Exception) {
 *     onError(FirebaseExceptionMapper.map(e))
 * }
 * ```
 *
 * Usage dans un ViewModel avec Result<T>.onFailure { err: Throwable } :
 * ```kotlin
 * .onFailure { err -> _state.value = UiState.Error(FirebaseExceptionMapper.map(err)) }
 * ```
 */
object FirebaseExceptionMapper {

    /**
     * Surcharge Throwable — utilisée quand l'appelant reçoit un [Throwable]
     * (ex : Result<T>.onFailure, coroutines qui remontent une cause non-Exception).
     */
    fun map(t: Throwable): AppException = when (t) {
        is AppException -> t
        is Exception    -> map(t)
        else            -> AppException.Unknown(t)
    }

    fun map(e: Exception): AppException = when (e) {
        is AppException               -> e
        is FirebaseAuthException      -> mapAuth(e)
        is FirebaseFirestoreException -> mapFirestore(e)
        is StorageException           -> mapStorage(e)
        is FirebaseNetworkException   -> AppException.NetworkUnavailable()
        is java.net.SocketTimeoutException,
        is java.net.UnknownHostException -> AppException.NetworkUnavailable()
        is IllegalStateException -> when {
            e.message?.contains("connecté", ignoreCase = true) == true -> AppException.NotAuthenticated()
            else -> AppException.Unknown(e)
        }
        else -> AppException.Unknown(e)
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    private fun mapAuth(e: FirebaseAuthException): AppException = when (e.errorCode) {
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_EMAIL"          -> AppException.InvalidCredentials()
        "ERROR_EMAIL_ALREADY_IN_USE"   -> AppException.EmailAlreadyInUse()
        "ERROR_USER_NOT_FOUND"         -> AppException.UserNotFound()
        "ERROR_WEAK_PASSWORD"          -> AppException.WeakPassword()
        "ERROR_TOO_MANY_REQUESTS"      -> AppException.TooManyRequests()
        "ERROR_NETWORK_REQUEST_FAILED" -> AppException.NetworkUnavailable()
        "ERROR_USER_DISABLED",
        "ERROR_OPERATION_NOT_ALLOWED"  -> AppException.PermissionDenied()
        else                           -> AppException.Unknown(e)
    }

    // ── Firestore ─────────────────────────────────────────────────────────────

    private fun mapFirestore(e: FirebaseFirestoreException): AppException = when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED   -> AppException.PermissionDenied()
        FirebaseFirestoreException.Code.NOT_FOUND         -> AppException.DocumentNotFound()
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.ABORTED           -> AppException.NetworkUnavailable()
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppException.Timeout()
        FirebaseFirestoreException.Code.ALREADY_EXISTS    -> AppException.EmailAlreadyInUse()
        FirebaseFirestoreException.Code.DATA_LOSS,
        FirebaseFirestoreException.Code.INTERNAL          -> AppException.Unknown(e)
        else                                              -> AppException.Unknown(e)
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    private fun mapStorage(e: StorageException): AppException = when (e.errorCode) {
        StorageException.ERROR_QUOTA_EXCEEDED       -> AppException.StorageQuotaExceeded()
        StorageException.ERROR_NOT_AUTHORIZED       -> AppException.PermissionDenied()
        StorageException.ERROR_OBJECT_NOT_FOUND     -> AppException.StorageObjectNotFound()
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> AppException.Timeout()
        StorageException.ERROR_BUCKET_NOT_FOUND,
        StorageException.ERROR_PROJECT_NOT_FOUND    -> AppException.Unknown(e)
        else                                        -> AppException.Unknown(e)
    }
}