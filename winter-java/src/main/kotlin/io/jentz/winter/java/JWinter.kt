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
    ): TypeKey<Unit, R> = ClassTypeKey(type, qualifier)

    /**
     * Creates a [TypeKey] with argument type [A] and return type [R] from Java classes.
     *
     *
     * @param argumentType The argument type.
     * @param returnType The return type.
     * @param qualifier The optional qualifier.
     * @return The type key.
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> key(
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null
    ): TypeKey<A, R> = CompoundClassTypeKey(argumentType, returnType, qualifier)

    /**
     * @see Graph.instance
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> instance(graph: Graph, type: Class<R>, qualifier: Any? = null): R =
        graph.instanceByKey(key(type, qualifier))

    /**
     * @see Graph.instance
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> instance(
        graph: Graph,
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null,
        argument: A
    ): R = graph.instanceByKey(key(argumentType, returnType, qualifier), argument)

    /**
     * @see Graph.instanceOrNull
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> instanceOrNull(graph: Graph, type: Class<R>, qualifier: Any? = null): R? =
        graph.instanceOrNullByKey(key(type, qualifier))

    /**
     * @see Graph.instanceOrNull
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> instanceOrNull(
        graph: Graph,
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null,
        argument: A
    ): R? = graph.instanceOrNullByKey(key(argumentType, returnType, qualifier), argument)

    /**
     * @see Graph.provider
     */
    @JvmStatic
    @JvmOverloads
    fun <R : Any> provider(graph: Graph, type: Class<R>, qualifier: Any? = null): Provider<R> =
        graph.providerByKey(key(type, qualifier))

    /**
     * @see Graph.provider
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> provider(
        graph: Graph,
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null,
        argument: A
    ): Provider<R> = graph.providerByKey(key(argumentType, returnType, qualifier), argument)

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
     * @see Graph.providerOrNull
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> providerOrNull(
        graph: Graph,
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null,
        argument: A
    ): Provider<R>? = graph.providerOrNullByKey(key(argumentType, returnType, qualifier), argument)

    /**
     * @see Graph.factory
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> factory(
        graph: Graph,
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null
    ): Factory<A, R> = graph.factoryByKey(key(argumentType, returnType, qualifier))

    /**
     * @see Graph.factoryOrNull
     */
    @JvmStatic
    @JvmOverloads
    fun <A, R : Any> factoryOrNull(
        graph: Graph,
        argumentType: Class<A>,
        returnType: Class<R>,
        qualifier: Any? = null
    ): Factory<A, R>? = graph.factoryOrNullByKey(key(argumentType, returnType, qualifier))

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
