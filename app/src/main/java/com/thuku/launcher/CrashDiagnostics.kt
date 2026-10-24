package com.thuku.launcher

import android.content.Context
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/** Local-only diagnostics for recovering from unexpected launcher errors. */
internal object CrashDiagnostics {
    private const val FILE_NAME = "thuku-diagnostics.txt"
    private const val MAX_REPORT_CHARS = 80_000

    @Volatile
    private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            record(appContext, "Uncaught exception on thread '${thread.name}'", throwable)
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
        installed = true
    }

    fun record(context: Context, title: String, error: Throwable?) {
        runCatching {
            val appContext = context.applicationContext
            val details = if (error == null) "No exception details were provided." else buildString {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                append(sw.toString())
            }
            val report = buildString {
                appendLine("Thuku Launcher diagnostics")
                appendLine("Time UTC/local device time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())}")
                appendLine("Issue: ${title.take(300)}")
                appendLine("Android SDK: ${android.os.Build.VERSION.SDK_INT}")
                appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".take(300))
                appendLine()
                append(details.take(MAX_REPORT_CHARS))
            }.take(MAX_REPORT_CHARS)
            val target = appContext.getFileStreamPath(FILE_NAME)
            val temp = appContext.getFileStreamPath("$FILE_NAME.tmp")
            temp.writeText(report)
            if (!temp.renameTo(target)) {
                target.writeText(report)
                temp.delete()
            }
        }
    }

    fun read(context: Context): String? = runCatching {
        context.applicationContext.getFileStreamPath(FILE_NAME)
            .takeIf { it.isFile && it.length() > 0L }
            ?.readText()
            ?.take(MAX_REPORT_CHARS)
    }.getOrNull()

    fun clear(context: Context): Boolean = runCatching {
        val file = context.applicationContext.getFileStreamPath(FILE_NAME)
        !file.exists() || file.delete()
    }.getOrDefault(false)
}
