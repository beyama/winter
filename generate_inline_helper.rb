new_methods = []
single_of_methods = []
proto_of_methods = []

(0 .. 21).each do |args|
    generic_parameters = ["reified R: Any"] + (0 ... args).map {|i| "reified A#{i}" }
    constructor_parameters = (0 ... args).map {|i| "a#{i}: A#{i}"}

    sig = "inline fun <" 
    sig += generic_parameters.join(", ") 
    sig += "> Graph.new(noinline constructor: (" 
    sig += constructor_parameters.join(", ") 
    sig += ") -> R): R"
    resolver = (0 ... args).map { "instance()" }
    body = "    constructor(" + resolver.join(", ") + ")"

    method = sig + " =\n"
    method += body

    new_methods << method

    sig = "inline fun <" 
    sig += generic_parameters.join(", ") 
    sig += "> Component.Builder.singletonOf("
    sig +="noinline constructor: ("
    sig += constructor_parameters.join(", ")
    sig += ") -> R, "
    sig += "qualifier: Any? = null, "
    sig += "generics: Boolean = false, "
    sig += "override: Boolean = false)"

    body = "    singleton(qualifier, generics, override) { new(constructor) }"

    method = sig + " =\n"
    method += body

    single_of_methods << method

    sig = "inline fun <" 
    sig += generic_parameters.join(", ") 
    sig += "> Component.Builder.prototypeOf("
    sig +="noinline constructor: ("
    sig += constructor_parameters.join(", ")
    sig += ") -> R, "
    sig += "qualifier: Any? = null, "
    sig += "generics: Boolean = false, "
    sig += "override: Boolean = false)"

    body = "    prototype(qualifier, generics, override) { new(constructor) }"

    method = sig + " =\n"
    method += body

    proto_of_methods << method
end

graph_new = <<-SRC
package io.jentz.winter.dsl

import io.jentz.winter.Graph

/**
* Create an instance of type [R]. 
* All arguments will be resolved from this graph.
*
* This uses inline function an no reflection.
* Inspired by https://insert-koin.io .
*/
#{new_methods.join("\n\n")}

SRC

single_of = <<-SRC
package io.jentz.winter.dsl

import io.jentz.winter.Component

/**
* Register a singleton scoped constructor for an instance of type [R].
*
* @param constructor The constructor of type [R].
* @param qualifier An optional qualifier.
* @param generics If true this will preserve generic information of [R].
* @param override If true this will override a existing provider of this type.
*/
#{single_of_methods.join("\n\n")}

SRC

proto_of = <<-SRC
package io.jentz.winter.dsl

import io.jentz.winter.Component

/**
* Register a prototype scoped constructor for an instance of type [R].
*
* @param constructor The constructor of type [R].
* @param qualifier An optional qualifier.
* @param generics If true this will preserve generic information of [R].
* @param override If true this will override a existing provider of this type.
*/
#{proto_of_methods.join("\n\n")}

SRC

File.open("winter/src/main/kotlin/io/jentz/winter/dsl/GraphNew.kt", "w") do |f|
    f.puts graph_new
end

File.open("winter/src/main/kotlin/io/jentz/winter/dsl/ComponentBuilderSingletonOf.kt", "w") do |f|
    f.puts single_of
end

File.open("winter/src/main/kotlin/io/jentz/winter/dsl/ComponentBuilderPrototypeOf.kt", "w") do |f|
    f.puts proto_of
end
