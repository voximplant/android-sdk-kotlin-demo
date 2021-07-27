package com.voximplant.demos.kotlin.utils

import android.content.Context

class GetResource(private val context: Context) {
    fun getString(string: Int): String {
        return context.getString(string)
    }
}