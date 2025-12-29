package com.example.swimminganalysisapplication.util

import android.util.Log
import com.example.swimminganalysisapplication.BuildConfig

/**
 * アプリ全体のログ出力を制御するユーティリティ
 * リリースビルドでは自動的にログが無効化される
 */
object AppLog {
    private const val ENABLED = BuildConfig.ENABLE_LOGGING

    fun d(tag: String, message: String) {
        if (ENABLED) Log.d(tag, message)
    }

    fun e(tag: String, message: String) {
        if (ENABLED) Log.e(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (ENABLED) Log.e(tag, message, throwable)
    }

    fun w(tag: String, message: String) {
        if (ENABLED) Log.w(tag, message)
    }

    fun i(tag: String, message: String) {
        if (ENABLED) Log.i(tag, message)
    }

    fun v(tag: String, message: String) {
        if (ENABLED) Log.v(tag, message)
    }
}
