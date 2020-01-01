package io.jentz.winter.android.test

import android.app.Activity
import android.app.Application
import io.jentz.winter.Component.Builder.SubcomponentIncludeMode.DoNotInclude
import io.jentz.winter.Winter
import io.jentz.winter.android.generatedComponent
import io.jentz.winter.android.test.quotes.QuotesAdapter
import io.jentz.winter.android.test.quotes.QuotesViewModel
import io.jentz.winter.android.test.quotes.QuotesViewState
import io.jentz.winter.android.test.scope.ActivityScope
import io.jentz.winter.android.test.scope.ApplicationScope
import io.jentz.winter.android.test.viewmodel.ViewModel
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