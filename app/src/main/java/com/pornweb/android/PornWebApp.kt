package com.pornweb.android

import android.app.Application
import coil.Coil
import com.pornweb.android.data.AppContainer

class PornWebApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Coil.setImageLoader(container.imageLoader)
    }
}
