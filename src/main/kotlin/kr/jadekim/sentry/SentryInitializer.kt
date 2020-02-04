package kr.jadekim.sentry

import io.sentry.Sentry
import java.util.*

fun initSentry(dsn: String, serviceEnv: String, version: String) {
    Sentry.init(dsn).apply {
        environment = serviceEnv
        release = version
    }
}

fun initSentry(properties: Properties, serviceEnv: String) = initSentry(
    properties.getProperty("sentry.dsn"),
    serviceEnv,
    properties.getProperty("DEPLOY_VERSION", "not_set")
)