package io.jentz.winter.androidx.docs

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import io.jentz.winter.Component
import io.jentz.winter.Winter
import io.jentz.winter.androidx.ActivityScope
import io.jentz.winter.androidx.ViewModelScope
import io.jentz.winter.androidx.dsl.activityGraphResolver
import io.jentz.winter.androidx.dsl.androidLifecycle
import io.jentz.winter.androidx.dsl.androidProcessLifecycle
import io.jentz.winter.androidx.dsl.application
import io.jentz.winter.component
import io.jentz.winter.delegate.provideDelegate
import io.jentz.winter.dsl.singletonOf

/*
 * The Android samples of the guide (doc/index.adoc). They are only compiled, not run.
 *
 * The guide includes the regions between the `tag::name[]` and `end::name[]` comments.
 */

class GitHubApi
class RepoListViewModel(val api: GitHubApi)
class ImageLoader(val activity: Activity)
class RepoId(val value: String)

class SyncManager {
    fun resume() = Unit
    fun pause() = Unit
}

class LocationTracker {
    fun start() = Unit
    fun stop() = Unit
}

// tag::application[]
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Winter.component {
            singletonOf(::GitHubApi)

            // creates the view model and activity subgraphs for RepoListActivity
            activityGraphResolver<RepoListActivity>()

            // survives configuration changes and is closed when the activity is finished
            subcomponent(ViewModelScope) {
                singletonOf(::RepoListViewModel)

                // created for every activity instance and closed when it is destroyed
                subcomponent(ActivityScope) {
                    singleton { ImageLoader(instance()) }
                }
            }
        }

        Winter.openGraph {
            // registers the Application, its Context and the process lifecycle
            application(this@MyApplication)
        }
    }
}
// end::application[]

// tag::activity[]
class RepoListActivity : ComponentActivity() {
    private val injector by Winter
    private val viewModel: RepoListViewModel by injector()
    private val imageLoader: ImageLoader by injector()

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject()
        super.onCreate(savedInstanceState)
    }
}
// end::activity[]

class RepoDetailActivity : ComponentActivity()

fun resolverBlocks(): Component = component {
    // tag::resolverBlocks[]
    activityGraphResolver<RepoDetailActivity>(
        // called once per view model subgraph, don't keep a reference to the activity here
        viewModelScope = { activity ->
            constant(RepoId(activity.intent.getStringExtra("repoId")!!))
        },
        // called for every activity instance
        activityScope = { activity ->
            constant<Intent>(activity.intent)
        }
    )
    // end::resolverBlocks[]
}

fun lifecycle(): Component = component {
    // tag::lifecycle[]
    // follows the process lifecycle (application component)
    singleton { SyncManager() }
        .androidProcessLifecycle(onStart = { it.resume() }, onStop = { it.pause() })

    subcomponent(ViewModelScope) {
        subcomponent(ActivityScope) {
            // follows the lifecycle of the activity
            singleton { LocationTracker() }
                .androidLifecycle(onStart = { it.start() }, onStop = { it.stop() })
        }
    }
    // end::lifecycle[]
}
