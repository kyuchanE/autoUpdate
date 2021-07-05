package com.uplusupdate.base

import android.app.Application
import com.uplusupdate.model.K
import com.uplusupdate.utils.hawk
import com.uplusupdate.utils.save
import com.facebook.stetho.Stetho
import com.orhanobut.hawk.Hawk

/**
 *  BaseApplication
 *  공통 베이스 어플리케이션
 *  by chan9u
 *  last 20.11.22
 */
class BaseApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)
        // hawk init
        Hawk.init(this).build()
        if (hawk(K.hawk.periodic, "").isNullOrEmpty()) {
            "".save(K.hawk.periodic)
        }

    }
}