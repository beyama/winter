package io.jentz.winter.java

import io.jentz.winter.*

object JWinter {

    /**
     * Creates a [TypeKey] with argument type Unit and return type [R] from a Java class.
     *
     * @param type The return type.
     * @param qualifier The optional qualifier.
     * @return The type key.
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> key(
        type: Class<R>,
        qualifier: Any? = null
    ): TypeKey<R> = ClassTypeKey(type, qualifier)

    /**
     * @see Graph.instance
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> instance(graph: Graph, type: Class<R>, qualifier: Any? = null): R =
        graph.instanceByKey(key(type, qualifier))

    /**
     * @see Graph.instanceOrNull
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> instanceOrNull(graph: Graph, type: Class<R>, qualifier: Any? = null): R? =
        graph.instanceOrNullByKey(key(type, qualifier))

    /**
     * @see Graph.provider
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> provider(graph: Graph, type: Class<R>, qualifier: Any? = null): Provider<R> =
        graph.providerByKey(key(type, qualifier))

    /**
     * @see Graph.providerOrNull
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> providerOrNull(
        graph: Graph,
        type: Class<R>,
        qualifier: Any? = null
    ): Provider<R>? = graph.providerOrNullByKey(key(type, qualifier))

    /**
     * @see Graph.instancesOfType
     */
    @JvmStatic
    fun <R: Any> instancesOfType(graph: Graph, type: Class<R>): Set<R> =
        graph.instancesOfTypeByKey(key(type, TYPE_KEY_OF_TYPE_QUALIFIER))

    /**
     * @see Graph.providersOfType
     */
    @JvmStatic
    fun <R: Any> providersOfType(graph: Graph, type: Class<R>): Set<Provider<R>> =
        graph.providersOfTypeByKey(key(type, TYPE_KEY_OF_TYPE_QUALIFIER))

}
