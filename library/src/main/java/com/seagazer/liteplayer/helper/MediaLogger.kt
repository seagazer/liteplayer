package com.seagazer.liteplayer.helper

import android.util.Log
import com.seagazer.liteplayer.BuildConfig

/**
 * Logger helper for player.
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
class MediaLogger {
    enum class Level(val l: Int) {
        DEBUG(0), INFO(1), WARNING(2), ERROR(3)
    }

    companion object {
        private const val TAG = "Seagazer"
        private var DEBUG = BuildConfig.DEBUG
        private var openLogger = false
        private var level = Level.DEBUG

        fun openLogger() {
            openLogger = true
        }

        fun setLevel(l: Level) {
            level = l
        }

        fun isOpenLogger(): Boolean {
            return DEBUG || openLogger
        }

        fun d(msg: String) {
            if ((DEBUG || openLogger) && level <= Level.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.d(TAG, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun i(msg: String) {
            if ((DEBUG || openLogger) && level <= Level.INFO) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.i(TAG, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun w(msg: String) {
            if ((DEBUG || openLogger) && level <= Level.WARNING) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.w(TAG, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun e(msg: String) {
            if ((DEBUG || openLogger) && level <= Level.ERROR) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.e(TAG, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun d(tag: String, msg: String) {
            if ((DEBUG || openLogger) && level <= Level.DEBUG) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.d(tag, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun i(tag: String, msg: String) {
            if ((DEBUG || openLogger) && level <= Level.INFO) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.i(tag, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun w(tag: String, msg: String) {
            if ((DEBUG || openLogger) && level <= Level.WARNING) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.w(tag, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }

        fun e(tag: String, msg: String) {
            if ((DEBUG || openLogger) && level <= Level.ERROR) {
                val stackTrace = Thread.currentThread().stackTrace
                if (stackTrace.size > 4) {
                    val e = stackTrace[3]
                    Log.e(tag, "${e.className} # ${e.methodName}[Line:${e.lineNumber}]: $msg")
                }
            }
        }
    }
}