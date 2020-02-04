package kr.jadekim.sentry.coroutine

import io.sentry.Sentry
import io.sentry.context.Context
import io.sentry.event.Breadcrumb
import io.sentry.event.User
import io.sentry.event.interfaces.HttpInterface
import kotlinx.coroutines.ThreadContextElement
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class SentryContext(
    sentryContext: Context? = Sentry.getContext()
) : ThreadContextElement<SentryData?>, AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<SentryContext>

    val data = sentryContext?.toSentryData()

    override fun updateThreadContext(context: CoroutineContext): SentryData? {
        val oldState = Sentry.getContext()?.toSentryData()

        updateSentryContext(data)

        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: SentryData?) {
        updateSentryContext(oldState)
    }

    private fun Context.toSentryData(): SentryData {
        return SentryData(breadcrumbs, extra, http, lastEventId, tags, user)
    }

    private fun updateSentryContext(data: SentryData?) {
        Sentry.clearContext()

        if (data == null) {
            return
        }

        Sentry.getContext().apply {
            http = data.http
            lastEventId = data.lastEventId
            user = data.user
            data.breadcrumbs?.forEach { recordBreadcrumb(it) }
            data.extra?.forEach { k, v -> addExtra(k, v) }
            data.tags?.forEach { k, v -> addTag(k, v) }
        }
    }
}

data class SentryData(
    val breadcrumbs: List<Breadcrumb>?,
    val extra: Map<String, Any>?,
    val http: HttpInterface?,
    val lastEventId: UUID?,
    val tags: Map<String, String>?,
    val user: User?
)