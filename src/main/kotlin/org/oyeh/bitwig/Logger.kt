package org.oyeh.bitwig

import com.bitwig.extension.controller.api.ControllerHost
import java.text.SimpleDateFormat
import java.util.*

enum class LogSeverity(val isError: Boolean) {
    TRACE(isError = false),
    DEBUG(isError = false),
    INFO(isError = false),
    IMPORTANT(isError = false),
    WARN(isError = false),
    ERROR(isError = true),
    FATAL(isError = true)
}

class Console(
    private val host: ControllerHost,
) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        .let { formatter ->
            { formatter.format(Date()) }
        }

    private fun StringBuilder.appendLogTag(tag: String): StringBuilder =
        this.append("[").append(tag).append("]")

    fun formatMessage(message: String, vararg messageTags: String) =
        StringBuilder(1024)
            .appendLogTag(timestamp())
            .also { builder ->
                messageTags.forEach { builder.appendLogTag(it) }
            }
            .append(": ") .append(message)
            .toString()

    fun log(message: String, vararg messageTags: String) =
        host.println(formatMessage(message, *messageTags))

    fun logError(message: String, vararg messageTags: String) =
        host.errorln(formatMessage(message, *messageTags))
}

class PopupNotification(private val host: ControllerHost) {
    fun show(message: String) {
        host.showPopupNotification(message)
    }
}

interface ILogger {
    fun log(severity: LogSeverity, message: String, vararg tags: String)
}

class Logger private constructor(
    val module: String?,
    var minimumSeverity: LogSeverity,
    private val console: Console,
    private val _popup: PopupNotification
) : ILogger {

    constructor(host: ControllerHost, minimumSeverity: LogSeverity)
            : this(null, minimumSeverity, Console(host), PopupNotification(host))

    var logPopupsToConsole: Boolean = true

    val popup: ILogger = object : ILogger {
        override fun log(severity: LogSeverity, message: String, vararg tags: String) {
            _popup.show(message)
            if (logPopupsToConsole)
                this@Logger.log(severity, message, *tags)
        }
    }

    fun loggerFor(submodule: String, severity: LogSeverity = this.minimumSeverity): Logger {
        val newModule = module?.let { "${module}.${submodule}" } ?: submodule
        return Logger(newModule, severity, console, _popup).also {
            it.logPopupsToConsole = logPopupsToConsole
        }
    }

    fun canPrint(severity: LogSeverity) =
        severity.ordinal >= this.minimumSeverity.ordinal

    override fun log(severity: LogSeverity, message: String, vararg tags: String) {
        if (!canPrint(severity))
            return

        val severityTag = severity.name
        val _tags = module
            ?.let { arrayOf(severityTag, module, *tags)}
            ?: arrayOf(severityTag, *tags)

        when {
            severity.isError -> console.logError(message, *_tags)
            else -> console.log(message, *_tags)
        }
    }

    inline fun log(severity: LogSeverity, messageBuilder: () -> String, vararg tags: String) {
        if (canPrint(severity))
            log(severity, messageBuilder(), *tags)
    }

    fun trace(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.TRACE, messageBuilder(), *tags)

    fun debug(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.DEBUG, messageBuilder(), *tags)

    fun info(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.INFO, messageBuilder(), *tags)

    fun important(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.IMPORTANT, messageBuilder(), *tags)

    fun warn(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.WARN, messageBuilder(), *tags)

    fun error(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.ERROR, messageBuilder(), *tags)

    fun fatal(messageBuilder: () -> String, vararg tags: String) =
        log(LogSeverity.FATAL, messageBuilder(), *tags)
}


fun ILogger.trace(message: String, vararg tags: String) =
    log(LogSeverity.TRACE, message, *tags)

fun ILogger.debug(message: String, vararg tags: String) =
    log(LogSeverity.DEBUG, message, *tags)

fun ILogger.info(message: String, vararg tags: String) =
    log(LogSeverity.INFO, message, *tags)

fun ILogger.important(message: String, vararg tags: String) =
    log(LogSeverity.IMPORTANT, message, *tags)

fun ILogger.warn(message: String, vararg tags: String) =
    log(LogSeverity.WARN, message, *tags)

fun ILogger.error(message: String, vararg tags: String) =
    log(LogSeverity.ERROR, message, *tags)

fun ILogger.fatal(message: String, vararg tags: String) =
    log(LogSeverity.FATAL, message, *tags)
