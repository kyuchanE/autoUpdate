package com.chan9u.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.gson.JsonObject
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable


/**
 *  BaseActivity
 *  공통 베이스 엑티비티
 *  by chan9u
 *  last 20.11.20
 */
abstract class BaseActivity<B: ViewDataBinding>: AppCompatActivity() {

    // dataBinding
    protected lateinit var binding: B

    // layoutId
    abstract val layoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, layoutId)
    }

    // initView
    open fun initViews() {}

}