package io.jentz.winter.androidx

import android.app.Application
import android.support.multidex.MultiDexApplication
import io.jentz.winter.Winter
import io.jentz.winter.adapter.useApplicationGraphOnlyAdapter
import io.jentz.winter.emptyComponent

class TestApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        Winter.useApplicationGraphOnlyAdapter()
        Winter.component = emptyComponent()
        Winter.open {
            constant<Application>(this@TestApp)
        }
    }

}
