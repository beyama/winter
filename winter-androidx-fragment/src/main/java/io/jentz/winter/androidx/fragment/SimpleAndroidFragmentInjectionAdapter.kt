package io.jentz.winter.androidx.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.SimpleAndroidInjectionAdapter
import io.jentz.winter.androidx.fragment.inject.FragmentScope

/**
 * Extended version of [SimpleAndroidInjectionAdapter] that has support for fragments and provides
 * a variety of subtypes and components of the activity graphs activity.
 *
 * In addition to the types provided by the base adapter this one provides:
 * * the activity as [FragmentActivity] if the activity is an instance of [FragmentActivity]
 * * the activities [androidx.fragment.app.FragmentManager] if the activity is an instance of
 *   [FragmentActivity]
 * * a [WinterFragmentFactory] if [enableWinterFragmentFactory] is true and activity is an
 *   instance of [FragmentActivity]
 */
open class SimpleAndroidFragmentInjectionAdapter(
    app: WinterApplication,
    private val enableWinterFragmentFactory: Boolean = false
) : SimpleAndroidInjectionAdapter(app) {

    override fun get(instance: Any): Graph? {
        if (instance is Fragment) return getFragmentGraph(instance)
        return super.get(instance)
    }

    override fun close(instance: Any) {
        if (instance is Fragment) closeFragmentGraph(instance)
        else super.close(instance)
    }

    protected open fun getFragmentGraph(fragment: Fragment): Graph? {
        val activity = checkNotNull(fragment.activity) {
            "Fragment is not attached to an activity so we cannot get its dependency graph"
        }
        return getActivityGraph(activity)?.getOrOpenSubgraph(FragmentScope::class, fragment) {
            provideAndroidTypes(fragment, this)
            setupAutoClose(fragment)
        }
    }

    protected open fun closeFragmentGraph(fragment: Fragment) {
        if (fragment.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        getFragmentGraph(fragment)?.close()
    }

    override fun provideAndroidTypes(instance: Any, builder: Component.Builder) {
        super.provideAndroidTypes(instance, builder)

        with(builder) {
            if (instance is Fragment) {
                constant(instance)
            }

            if (instance is FragmentActivity) {
                constant(instance)
                constant(instance.supportFragmentManager)

                if (enableWinterFragmentFactory) {
                    eagerSingleton(
                        onPostConstruct = { instance<FragmentManager>().fragmentFactory = it }
                    ) { WinterFragmentFactory(this) }
                }
            }
        }
    }
}

/**
 * Register an [SimpleAndroidFragmentInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useSimpleAndroidFragmentAdapter(
    enableWinterFragmentFactory: Boolean = false
) {
    injectionAdapter = SimpleAndroidFragmentInjectionAdapter(this, enableWinterFragmentFactory)
}
