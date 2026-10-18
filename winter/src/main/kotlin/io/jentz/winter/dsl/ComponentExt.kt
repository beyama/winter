package io.jentz.winter.dsl

import io.jentz.winter.Component
import io.jentz.winter.Component.Builder
import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.Qualifier

/**
 * Returns a subcomponent by its path.
 *
 * Main usage for this is to restructure components when using [Builder.include].
 *
 * @param path The path of qualifiers of the subcomponent.
 * @return The subcomponent
 *
 * @throws EntryNotFoundException If the component does not exist.
 */
fun Component.subcomponent(path: List<Qualifier>): Component =
    path.fold(this) { component, qualifier -> component.subcomponent(qualifier )}