package io.jentz.winter.android.sample

import android.app.Activity
import android.app.Application
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.DoNotInclude
import io.jentz.winter.Winter
import io.jentz.winter.android.sample.quotes.QuotesAdapter
import io.jentz.winter.android.sample.quotes.QuotesViewModel
import io.jentz.winter.android.sample.quotes.QuotesViewState
import io.jentz.winter.android.sample.scope.ActivityScope
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.android.sample.viewmodel.ViewModel
import io.jentz.winter.androidx.useAndroidPresentationScopeAdapter
import io.jentz.winter.rxjava2.installDisposablePlugin

class IntegrationTestApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Winter.component {
            include(generatedComponent, subcomponentIncludeMode = DoNotInclude)
            include(
                generatedComponent.subcomponent(ApplicationScope::class),
                subcomponentIncludeMode = DoNotInclude
            )

            subcomponent("presentation") {
                singleton<ViewModel<QuotesViewState>>(generics = true) {
                    QuotesViewModel(instance())
                }

                subcomponent("activity") {
                    include(generatedComponent.subcomponent(ActivityScope::class))

                    prototype { QuotesAdapter(instance<Activity>().layoutInflater) }
                }
            }
        }
        Winter.installDisposablePlugin()
        Winter.useAndroidPresentationScopeAdapter()
        Winter.inject(this)
    }

}