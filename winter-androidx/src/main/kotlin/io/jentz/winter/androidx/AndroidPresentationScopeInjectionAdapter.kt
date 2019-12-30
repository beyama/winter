package io.jentz.winter.androidx

import android.app.Activity
import android.content.Context
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication

/**
 * Extended version of [SimpleAndroidInjectionAdapter] that retains a `presentation` sub graph
 * during Activity re-creation (configuration changes).
 *
 * It expects an application component like:
 *
 * ```
 * Winter.component {
 *   // this sub-graph outlives configuration changes and is only disposed when Activity
 *   // isFinishing == true
 *   subcomponent("presentation") {
 *     // this is recreated every time the Activity is recreated
 *     subcomponent("activity") {
 *     }
 *   }
 * }
 * Winter.useAndroidPresentationScopeAdapter()
 * ```
 */
open class AndroidPresentationScopeInjectionAdapter(
    winterApplication: WinterApplication
) : SimpleAndroidInjectionAdapter(winterApplication) {

    override fun getActivityGraph(activity: Activity): Graph? {
        val presentationIdentifier = presentationIdentifier(activity)
        app.getOrOpen("presentation", identifier = presentationIdentifier)

        app.getOrNull(presentationIdentifier, activity)?.let { return it }

        setupAutoClose(activity)

        return app.open(presentationIdentifier, "activity", identifier = activity) {
            constant(activity)
            constant<Context>(activity)
        }
    }

    override fun closeActivityGraph(activity: Activity) {
        if (activity.isFinishing) {
            app.close(presentationIdentifier(activity))
        } else {
            app.close(presentationIdentifier(activity), activity)
        }
    }

    private fun presentationIdentifier(activity: Activity) = activity.javaClass

}

/**
 * Register an [AndroidPresentationScopeInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeAdapter() {
    injectionAdapter = AndroidPresentationScopeInjectionAdapter(this)
}
