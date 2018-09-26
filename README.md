# Winter

[![Kotlin 1.1.71](https://img.shields.io/badge/Kotlin-1.1-blue.svg)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.jentz.winter/winter.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.jentz.winter%22)
[![Travis](https://travis-ci.org/beyama/winter.svg)](https://travis-ci.org/beyama/winter/builds)
[![MIT License](https://img.shields.io/github/license/beyama/winter.svg)](https://github.com/beyama/winter/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/beyama/winter.svg)](https://github.com/beyama/winter/issues)

## Kotlin Dependency Injection

Winter is a fast and intuitive dependency injection library for Kotlin on Android and the JVM.

It offers an idiomatic Kotlin API as well as optional JSR-330 support
with annotation processor.

See also the [API documentation for Winter](https://beyama.github.io/winter/javadoc/winter/io.jentz.winter/index.html) and [Winter Android](https://beyama.github.io/winter/javadoc/winter-android/io.jentz.winter.android/index.html).

## Getting Started

### Gradle

```groovy
dependencies {
  // Core
  implementation 'io.jentz.winter:winter:0.1.0'
  // Android support
  implementation 'io.jentz.winter:winter-android:0.1.0'
  // Optional JSR-330 support
  implementation 'javax.inject:javax.inject:1'
  kapt 'io.jentz.winter:winter-compiler:0.1.0'
}

// The optional JSR-330 support requires also a kapt configuration block like
kapt {
  arguments {
    // This tells the Winter compiler under which package name it should store 
    // the generated component. See the advanced section for more details.
    arg("winterGeneratedComponentPackage", "my.project.root.package.name")
  }
}
```

### Basic Concepts

The building blocks of Winter are:

| Term      | Definition                                                                                                                             |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------|
| Component | The immutable dependency registry for providers, factories and subcomponents.                                                          |
| Graph     | The "instance" of a component that holds actual instances and is used to retrieve dependencies defined in a component.                 |
| Scope     | The lifetime of an instance in a graph e.g. singleton for only one per graph or prototype for one each time an instance is requested.  |

### Simple Example

Given is an Android application with an application component and an
activity subcomponent. If you are more familiar with web development
then think of "request subcomponent" instead of "activity subcomponent".

```kotlin

// Definition of a component
val applicationComponent = component {
  singleton<HttpClient> { HttpClientImpl() }
  singleton<GitHubApi> { GitHubApiImpl(instance()) }

  subcompont("activity") {
    singleton { Glide.with(instance<Activity>()) }
  }
}

// Initialisation of a graph
val applicationGraph = applicationComponent.init {
  constant<Application>(myApplicationInstance)
}

// Initialisation of a graph of a subcomponent
val activityComponent = applicationGraph.initSubcomponent {
  constant<Activity>(myActivity)
}

// retrieval of a dependency
val gitHubApi: GitHubApi = applicationComponent.instance()

```

## Injector

The usage of the `Injector` class is the default way to handle
cases were you are not able to use constructor injection like in cases
were your application framework instantiates instances for you e.g.
Android activities and fragments.

It utilizes Kotlin property delegation and defers the dependency
retrieval to a point in time were you are able to create and provide a
dependency graph to the injector e.g. Activity#onCreate on Android.

I give you a simple example:

```kotlin

class MyActivity : Activity() {

    private val injector = Injector()
    // eager injection of a non-optional dependency
    private val api: GitHubApi by injector.instance()
    // eager injection of an optional dependency
    private val api: GitHubApi? by injector.instanceOrNull()
    // lazy injection of a non-optional dependency
    private val api: GitHubApi by injector.lazyInstance()
    // lazy injection of an optional dependency
    private val api: GitHubApi? by injector.lazyInstanceOrNull()
    // injection of a non-optional factory
    private val factory: (Int) -> ProducedInstance by injector.factory()
    // injection of an optional factory
    private val factory: (Int) -> ProducedInstance by injector.factoryOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
      // ... create or get the dependency graph
      injector.inject(graph)
      super.onCreate(savedInstanceState)
    }
}
```

Lazy injection means that the actual retrieval and therefore the actual
instantiation of a dependency is deferred to the point where you access
the property the first time. This is useful in cases where the creation
is computationally expensive but may not be required in some cases.

## Graph Registry
The graph registry creates and holds dependency graphs in a tree (directed acyclic graph).

This was inspired by [Toothpick](https://github.com/stephanenicolas/toothpick).

For example:
```kotlin
GraphRegistry.applicationComponent = myApplicationComponent

// create the application dependency graph on application start
class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    GraphRegistry.open() { constant<Application> { this@MyApplication }
  }
}
// you can now retrieve the application dependency graph by calling
GraphRegistry.get()

// create and dispose a sub-graph of the application graph
class MyActivity : Activity() {
  override onCreate() {
    super.onCreate()
    // initialize subcomponent with name "activity" and register it with identifier this
    GraphRegistry.open("activity", identifier = this) { constant<Activity>(this@MyActivity) } 
  }
  
  override onDestroy() {
    super.onDestroy()
    // dispose the "activity" sub-graph with identifier this
    GraphRegistry.close(this)
  }
  
}
```

## Android Support

*TODO:* AndroidInjection, view-extensions

## Advanced Usage

*TODO:* mixing components, usage of factories, type erasure, qualifier

## JSR-330 Annotation Processor

JSR-330 support is provided by the Winter module `winter-compiler`.

Winter generates factories for your classes that have a @Inject
annotated constructor.

It generates a members-injector for each class that has @Inject annotated
setter or fields.

And it generates a component containing all those factories and
members-injectors to avoid the usage of reflection.

## License

    Copyright 2017 Alexander Jentz

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

