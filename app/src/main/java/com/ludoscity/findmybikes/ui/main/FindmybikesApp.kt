package com.ludoscity.findmybikes.ui.main

import android.app.Application
import android.content.Context
import com.ludoscity.findmybikes.common.di.initKoin
import org.koin.dsl.module

class FindmybikesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin (
            module {
                single<Context> { this@FindmybikesApp }
            }
        )
    }
}
