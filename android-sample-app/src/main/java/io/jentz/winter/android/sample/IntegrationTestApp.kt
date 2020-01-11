package io.jentz.winter.android.sample

import android.app.Activity
import android.app.Application
import android.view.LayoutInflater
import io.jentz.winter.Winter
import io.jentz.winter.android.sample.model.QuoteRepository
import io.jentz.winter.android.sample.quotes.QuoteFormatter
import io.jentz.winter.android.sample.quotes.QuotesAdapter
import io.jentz.winter.android.sample.quotes.QuotesViewModel
import io.jentz.winter.android.sample.quotes.QuotesViewState
import io.jentz.winter.android.sample.viewmodel.ViewModel
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.androidx.useAndroidPresentationScopeAdapter
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.rxjava2.installDisposablePlugin

class IntegrationTestApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Winter.component(ApplicationScope::class) {

            generated<QuoteRepository>()

            subcomponent(PresentationScope::class) {

                generated<QuotesViewModel>()
                    .alias<ViewModel<QuotesViewState>>(generics = true)

                subcomponent(ActivityScope::class) {

                    generated<QuoteFormatter>()
                    generated<QuotesAdapter>()

                    prototype<LayoutInflater> { instance<Activity>().layoutInflater }
                }
            }
        }
        Winter.installDisposablePlugin()
        Winter.useAndroidPresentationScopeAdapter()
        Winter.inject(this)
    }

}