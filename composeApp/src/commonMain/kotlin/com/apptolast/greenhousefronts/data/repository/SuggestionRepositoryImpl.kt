package com.apptolast.greenhousefronts.data.repository

import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionRequest
import com.apptolast.greenhousefronts.data.model.suggestion.CreateSuggestionResponse
import com.apptolast.greenhousefronts.data.remote.api.SuggestionApiService
import com.apptolast.greenhousefronts.domain.repository.SuggestionRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode

/**
 * `SuggestionRepository` implementation. Maps Ktor exceptions to user-friendly
 * Spanish error messages so the ViewModel can pipe `error.message` straight to
 * the snackbar without further branching. Same pattern AuthRepositoryImpl uses
 * for login.
 */
class SuggestionRepositoryImpl(
    private val apiService: SuggestionApiService,
) : SuggestionRepository {

    override suspend fun create(request: CreateSuggestionRequest): Result<CreateSuggestionResponse> {
        return try {
            Result.success(apiService.create(request))
        } catch (e: ClientRequestException) {
            // 4xx — body validation, auth, etc. Pre-validation in the
            // ViewModel should keep us off 400 in practice; 401 is handled
            // by the bearer plugin's refresh hook before reaching here.
            Result.failure(Throwable(mapClientErrorMessage(e.response.status)))
        } catch (e: ServerResponseException) {
            // 5xx — including the documented 502 "GitHub issue tracker
            // unavailable" path.
            Result.failure(Throwable("El servicio de soporte no está disponible ahora mismo. Inténtalo en unos minutos."))
        } catch (e: Exception) {
            // I/O, timeout, DNS, TLS, serialization…
            Result.failure(Throwable("Sin conexión. Comprueba tu red e inténtalo de nuevo."))
        }
    }

    private fun mapClientErrorMessage(status: HttpStatusCode): String = when (status) {
        HttpStatusCode.BadRequest ->
            "Por favor revisa los datos antes de enviar."
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
            "Tu sesión ha caducado. Vuelve a iniciar sesión."
        else ->
            "No se pudo enviar tu sugerencia (código ${status.value}). Inténtalo de nuevo."
    }
}
