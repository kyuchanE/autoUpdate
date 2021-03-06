package com.chan9u.base

import android.app.Application
import com.chan9u.model.K
import com.chan9u.utils.flash
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

        // 버전 정보 없을시 초기값 세팅
        if (Hawk.get<String>(K.hawk.contents_version).isNull &&
                Hawk.get<String>(K.hawk.apk_version).isNull){
            10000.save(K.hawk.contents_version)
            20002.save(K.hawk.apk_version)
        }

    }
}