package io.jentz.winter.androidx

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import io.jentz.winter.ClassTypeKey
import io.jentz.winter.Graph

/**
 * A [FragmentFactory] that resolves the fragment instances from the given [graph].
 * This allows us to use constructor injection for our [fragments][Fragment].
 *
 * This is usually used together with an Android
 * [io.jentz.winter.WinterApplication.InjectionAdapter].
 *
 * Example:
 * ```
 * // somewhere in the Android application class
 * Winter.useSimpleAndroidAdapter(enableWinterFragmentFactory = true)
 *
 * // in a FragmentActivity inject the factory; it is already setup by the adapter
 * @Inject lateinit var fragmentFactory: WinterFragmentFactory
 *
 * // add a fragment registered in the activity graph
 * supportFragmentManager
 *   .beginTransaction()
 *   .add(fragmentFactory.instance<MyFragment>(), "tag")
 *   .commit()
 * ```
 */
class WinterFragmentFactory(
    private val graph: Graph
) : FragmentFactory() {

    /**
     *  Get instance of fragment by fragment class [T] from activity graph.
     *
     *  @return The fragment instance.
     */
    inline fun <reified T : Fragment> instance(): T =
        instance(T::class.java)

    /**
     * Get instance of fragment by fragment class [clazz] from activity graph.
     *
     * @param clazz The fragment class.
     * @return The fragment instance
     */
    fun <T : Fragment> instance(clazz: Class<T>): T =
        graph.instanceByKey(ClassTypeKey(clazz))

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val clazz = loadFragmentClass(classLoader, className)
        return graph.instanceOrNullByKey(ClassTypeKey(clazz))
            ?: super.instantiate(classLoader, className)
    }

}
