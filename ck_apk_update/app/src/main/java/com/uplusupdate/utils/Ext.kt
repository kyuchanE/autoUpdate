package com.uplusupdate.utils

import android.util.Log
import com.orhanobut.hawk.Hawk

val Any?.notNull get() = this != null
val Any?.isNull get() = this == null

fun Any?.save(key: String) {
    if (this.isNull) {
        Hawk.delete(key)
        Log.d("@@@@Hawk delete :", "$key")
    } else {
        Hawk.put(key, this)
        Log.d("@@@Hawk save :", "$key = $this")
    }
}

fun <T> hawk(key: String): T = Hawk.get(key)
fun <T> hawk(key: String, default: T): T = Hawk.get(key, default)
fun <T> flash(key: String): T = Hawk.get<T>(key).also { Hawk.delete(key) }
fun <T> flash(key: String, default: T): T = Hawk.get(key, default).also { Hawk.delete(key) }