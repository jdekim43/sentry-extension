package kr.jadekim.sentry.ktor

import io.ktor.application.*
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.ContentType
import io.ktor.request.*
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import io.sentry.Sentry
import io.sentry.event.User
import io.sentry.event.UserBuilder
import io.sentry.event.interfaces.HttpInterface
import kotlinx.coroutines.withContext
import kr.jadekim.sentry.coroutine.SentryContext
import kr.jadekim.server.ktor.*
import java.net.InetAddress

data class UserInfo(
    val userIdx: Int,
    val userName: String = "",
    val email: String = "",
    val extra: Map<String, Any> = emptyMap()
) {

    fun toSentryUser(remoteIp: String?) = User(
        userIdx.toString(),
        userName,
        remoteIp,
        email,
        extra
    )
}

class KtorSentryIntegration private constructor(
    private val userInfoProvider: PipelineContext<Unit, ApplicationCall>.() -> UserInfo? = { null }
) {

    private val localHostname = InetAddress.getLocalHost().hostName

    class Configuration {
        var userInfoProvider: PipelineContext<Unit, ApplicationCall>.() -> UserInfo? = { null }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, KtorSentryIntegration> {

        override val key: AttributeKey<KtorSentryIntegration> = AttributeKey("SentryFeature")

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): KtorSentryIntegration {
            val configuration = Configuration().apply(configure)
            val feature = KtorSentryIntegration(configuration.userInfoProvider)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                val sentryContext = Sentry.getContext()

                val request = call.attributes.getOrNull(PATH)
                    ?: "${context.request.path()}(method:${context.request.httpMethod.value})"

                sentryContext.addTag("path", request)

                val method = call.request.httpMethod

                val parameters = (pathParam.toMap() + queryParam.toMap()).toMutableMap()

                if (method.canReadBody) {
                    when (call.request.contentType()) {
                        ContentType.Application.FormUrlEncoded -> {
                            try {
                                withContext(SentryContext(sentryContext)) {
                                    bodyParam()?.toMap()?.let {
                                        parameters.putAll(it)
                                    }
                                }
                            } catch (e: UnsupportedMediaTypeException) {
                                //do nothing
                            }
                        }
                        ContentType.Application.Json -> {
                            jsonBody().fields().forEach {
                                parameters[it.key] = listOf(it.value.asText())
                            }
                        }
                    }
                }

                sentryContext.http = HttpInterface(
                    call.request.uri,
                    method.value,
                    parameters.toMap(),
                    call.request.queryString(),
                    emptyMap(),
                    call.request.host(),
                    feature.localHostname,
                    call.request.port(),
                    call.request.local.host,
                    call.request.local.host,
                    call.request.local.port,
                    call.request.local.scheme + " " + call.request.local.version,
                    false,
                    false,
                    "",
                    "",
                    call.request.headers.toMap(),
                    ""
                )

                sentryContext.user = feature.userInfoProvider(this@intercept)?.toSentryUser(call.request.host())
                    ?: UserBuilder().setIpAddress(call.request.host()).build()

                withContext(SentryContext(sentryContext)) {
                    proceed()

                    Sentry.clearContext()
                }
            }

            return feature
        }
    }
}