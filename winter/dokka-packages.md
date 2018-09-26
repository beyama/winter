# Package io.jentz.winter

### Winter API

The building blocks of Winter are the `components` and the `graphs`.

#### Components

Components store the dependency provider; basically the dependency definitions provided to Winter - they are
the blueprints for the `graph`.

* they are immutable
* they can be extended (by using derive)
* they can be mixed (by using include in the builder)

#### Graphs

Graphs are used to retrieve and instantiate dependencies defined in `components`.

Think of Graphs as instances of `components`.
