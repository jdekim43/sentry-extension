package kr.jadekim.sentry

import io.sentry.Sentry
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import kr.jadekim.logger.model.Level
import kr.jadekim.logger.model.Log
import kr.jadekim.logger.printer.LogPrinter

class SentryLogPrinter(val printLevel: Level = Level.WARNING) : LogPrinter {

    override var printStackTrace: Boolean = true

    override fun print(log: Log) {
        if (!log.isPrintable(printLevel)) {
            return
        }

        if (log.loggerName.startsWith("io.sentry")) {
            return
        }

        val context = Sentry.getContext()

        val builder = EventBuilder()
            .withLevel(log.level.toSentryLevel())
            .withLogger(log.loggerName)
            .withMessage(log.message)
            .withTransaction(
                log.context["route"]?.toString()
                    ?: log.context["request"]?.toString()
                    ?: log.context["path"]?.toString()
                    ?: context.http?.requestUrl
            )
            .withSentryInterface(ExceptionInterface(log.throwable))

        log.extra.forEach { (k, v) -> builder.withExtra(k, v) }
        log.context.forEach { (k, v) -> builder.withExtra(k, v) }

        Sentry.capture(builder)
    }

    private fun Level.toSentryLevel(): Event.Level = when (this) {
        Level.TRACE, Level.DEBUG -> io.sentry.event.Event.Level.DEBUG
        Level.INFO -> io.sentry.event.Event.Level.INFO
        Level.WARNING -> io.sentry.event.Event.Level.WARNING
        Level.ERROR -> io.sentry.event.Event.Level.ERROR
    }
}