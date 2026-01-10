package com.yvartpro.dunda.ui.component

import android.util.Log

object Logger{
  private const val TAG = "Dunda"

  fun d(subTag: String, msg: String) {
    Log.d(TAG, "[$subTag] $msg")
  }

  fun e(subTag: String, msg: String, throwable: Throwable? = null) {
    Log.e(TAG, "[${subTag}] $msg", throwable)
  }
}