package com.chan9u.base

import android.app.Application
import com.chan9u.model.K
import com.chan9u.utils.flash
import com.chan9u.utils.hawk
import com.chan9u.utils.isNull
import com.chan9u.utils.save
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