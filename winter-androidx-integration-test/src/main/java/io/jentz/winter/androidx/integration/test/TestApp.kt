package io.jentz.winter.androidx.integration.test

import android.app.Application
import io.jentz.winter.Winter
import io.jentz.winter.adapter.useApplicationGraphOnlyAdapter
import io.jentz.winter.emptyComponent

@Suppress("unused")
class TestApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Winter.useApplicationGraphOnlyAdapter()
        Winter.component = emptyComponent()
        Winter.openGraph {
            constant<Application>(this@TestApp)
        }
    }

}
