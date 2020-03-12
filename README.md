# sentry-extension
* coroutine SentryContext
* j-logger SentryLogPrinter
* KtorSentryIntegration

## Install
### Gradle Project
1. Add dependency
    ```
    build.gradle.kts
   
    implementation("kr.jadekim:sentry-extension:1.0.0")
    ```

## How to use
### SentryContext
```
withContext(SentryContext()) {
    Sentry.capture(...)
}
```
### SentryLogPrinter
```
val loggingLevel = Level.WARNING
val printer = SentryLogPrinter(loggingLevel)
JLog.addPrinter(printer) //Do not use JLog.addAsyncPrinter. It can't get SentryContext.
```
### KtorSentryIntegration
```
build.gradle.kts

implementation("kr.jadekim:ktor-extension:$ktorExtensionVersion")
```
```
In ktor

install(KtorSentryIntegration) {
    userInfoProvider = {
        UserInfo(
            ...
        )
    }
}
```