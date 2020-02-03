package io.jentz.winter.android.sample

import android.app.Activity
import android.app.Application
import android.view.LayoutInflater
import io.jentz.winter.Winter
import io.jentz.winter.android.sample.GeneratedComponent.generatedComponent
import io.jentz.winter.android.sample.quotes.QuotesViewModel
import io.jentz.winter.android.sample.quotes.QuotesViewState
import io.jentz.winter.android.sample.viewmodel.ViewModel
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.androidx.useAndroidPresentationScopeAdapter
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.rxjava2.installDisposablePlugin
import io.jentz.winter.typeKey

class IntegrationTestApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Winter.component(ApplicationScope::class) {
            include(generatedComponent.subcomponent(ApplicationScope::class))

            subcomponent(PresentationScope::class) {
                include(generatedComponent.subcomponent(PresentationScope::class))

                alias(typeKey<QuotesViewModel>(), typeKey<ViewModel<QuotesViewState>>(generics = true))

                subcomponent(ActivityScope::class) {
                    include(generatedComponent.subcomponent(ActivityScope::class))
                    
                    prototype<LayoutInflater> { instance<Activity>().layoutInflater }
                }
            }
        }
        Winter.installDisposablePlugin()
        Winter.useAndroidPresentationScopeAdapter()
        Winter.inject(this)
    }

}