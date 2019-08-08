# Winter

[![Kotlin 1.3.40](https://img.shields.io/badge/Kotlin-1.3-blue.svg)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.jentz.winter/winter.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.jentz.winter%22)
[![Travis](https://travis-ci.org/beyama/winter.svg?branch=develop)](https://travis-ci.org/beyama/winter/builds)
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
  implementation 'io.jentz.winter:winter:0.3.0'
  // Android support
  implementation 'io.jentz.winter:winter-android:0.3.0'
  // Android X lifecycle extensions
  implementation 'io.jentz.winter:winter-androidx-lifecycle:0.3.0'
  // RxJava 2 disposable plugin
  implementation 'io.jentz.winter:winter-rxjava2:0.3.0'
  // JUnit 4 test support
  testImplementation 'io.jentz.winter:winter-junit4:0.3.0'  
  // Optional JSR-330 support
  implementation 'javax.inject:javax.inject:1'
  kapt 'io.jentz.winter:winter-compiler:0.3.0'
}

// The optional JSR-330 support requires also a kapt configuration block like
kapt {
  arguments {
    // Tell the Winter compiler under which package name it should generate the component
    arg("winterGeneratedComponentPackage", "my.project.root.package.name")
    // Optional: The custom scope annotation that is used as root scope instead of javax.inject.Singleton
    arg("winterRootScopeAnnotation", "my.project.root.package.name.ApplicationScope")
  }
}
```

### Basic Concepts

The building blocks of Winter are:

| Term          | Definition                                                                                                                               |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Component     | The immutable dependency registry for providers, factories and sub-components.                                                           |
| Sub-Component | A component that is defined inside a component used to create child graphs.                                                              |
| Graph         | The "instance" of a component that holds actual instances and is used to retrieve dependencies defined in a component.                   |
| Child-Graph   | Graph created from a sub-component that can access dependencies from its parent graph but the parent has no access to child dependencies.|
| Scope         | The lifetime of an instance in a graph e.g. singleton for only one per graph or prototype for one each time an instance is requested.    |

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
    singleton { Glide.with(instance()) }
  }
}

// Initialisation of the application graph
val applicationGraph = applicationComponent.init {
  constant<Application>(myApplicationInstance)
  constant<Context>(myApplicationInstance)
}

// Opening of a child graph
val activityGraph = applicationGraph.openChildGraph("activity") {
  constant<Activity>(myActivity)
  constant<Context>(myActivity)
}

// Retrieval of a dependency
val gitHubApi: GitHubApi = activityGraph.instance()

// Closing of the child graph
applicationGraph.closeChildGraph("activity")
// or directly on the child graph
activityGraph.dispose()

```

## Injector

It is considered the best way to use constructor based injection to have a consistent state after 
initialisation and proper encapsulation. 
But sometime classes are instantiated by the system, like Activities on Android. 

Then property injection is our only solution.

The usage of the `Injector` class is the recommended way to handle cases were you are not able to 
use constructor injection for your Kotlin classes.

It utilizes Kotlin property delegation and defers the dependency
retrieval to a point in time were you are able to provide a
dependency graph to the injector e.g. Activity#onCreate on Android.

Example:

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

In this example we see retrieval methods prefixed with lazy.
Lazy injection means that the actual retrieval and therefore the actual
instantiation of a dependency is deferred to the point where you access
the property the first time. This is useful in cases where the creation
is computationally expensive but may not be required in some cases.

For more details see [Injector API docs](https://winter.jentz.io/javadoc/winter/io.jentz.winter/-injector/index.html)

## Injection

We don't want knowledge of how to create or retrieve a dependency graph in our classes and therefor 
`Injection` was created. `Injection` allows us to create, get and dispose a dependency graph 
without having knowledge about the details.
The actual strategy to create, get and dispose a graph is part of an adapter.

Here is a basic example with the `SimpleAndroidInjectionAdapter` from the `winter-android` module:

```kotlin
class MyApplication : Application() {
  override fun onCreate() {
    // declare application component
    Winter.component {
      singleton<GitHubApi> { GitHubApiImpl() }
 
      singleton { RepoListViewModel(instance()) }

      subcomponent("activity") {
         singleton { Glide.with(instance<Activity>()) }
      }
    }

    /// Configure Injection to use the simple android adapter   
    Injection.useSimpleAndroidAdapter()
    // Create application graph by providing the application instance
    Injection.createGraph(this)
  }
}

class MyActivity : Activity() {
  private val injector = Injector()
  private val viewModel: RepoListViewModel by injector.instance()
  private val glide: RequestManager by injector.instance()

  override fun onCreate(savedInstanceState: Bundle?) {
    Injection.createGraphAndInject(this, injector)
    super.onCreate(savedInstanceState)
  }

  override fun onDestroy() {
    Injection.disposeGraph(this)
    super.onDestroy()
  }

}
```

## WinterAware

The `WinterAware` interface marks a class as aware of Winter and gives it access to a variety of
extension methods to get a dependency graph and to retrieve or inject dependencies.
 
A simple example:

```kotlin
class HomeScreen @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr), WinterAware {

  private val viewModel: HomeViewModel = instance()

}
```

*TODO:* Links to WinterAware, WinterAwareInjectionExt and WinterAwareGraphExt

## Android Support

The default `SimpleAndroidInjectionAdapter` is backed by `GraphRegistry` and can be easily extended.

For more details see [AndroidInjection API docs](https://winter.jentz.io/javadoc/winter-android/io.jentz.winter.android/-android-injection/index.html).

## JSR-330 Annotation Processor

JSR-330 support is provided by the Winter module `winter-compiler`.

Winter generates factories for your classes that have a @Inject
annotated constructor.

It generates a members-injector for each class that has @Inject annotated
setter or fields.

And it generates a component containing all those factories and
members-injectors to avoid the usage of reflection.

## Advanced Usage

*TODO:* mixing components, usage of factories, type erasure, qualifier

### WinterTree & GraphRegistry

A Graph can have multiple child-graphs and may have a parent graph which makes it a tree of graphs 
(directed acyclic graph).

`WinterTree` and its object version `GraphRegisty` are helper to create (open) and dispose (close) 
graphs by paths of component qualifier. 

This was inspired by [Toothpick](https://github.com/stephanenicolas/toothpick).

You can use `GraphRegistry` directly but it is usually a better approach to use the `Injection` 
abstraction and use `WinterTree` in an Adapter internally. 

For example:
```kotlin
GraphRegistry.applicationComponent = myApplicationComponent

// create the application dependency graph on application start
class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    
    // define a component with one sub-component
    Winter.component {
      subcomponent("activity") {
      }
    }
    
    GraphRegistry.open { constant<Application> { this@MyApplication } }
  }
}
// you can now retrieve the application dependency graph by calling
GraphRegistry.get()

// create and dispose a sub-graph of the application graph
class MyActivity : Activity() {
  override fun onCreate() {
    super.onCreate()
    // initialize subcomponent with name "activity" and register it with identifier this
    GraphRegistry.open("activity", identifier = this) { constant<Activity>(this@MyActivity) } 
  }
  
  override fun onDestroy() {
    super.onDestroy()
    // dispose the "activity" sub-graph with identifier this
    GraphRegistry.close(this)
  }
  
}
```

If you close (dispose) a graph it will also close all registered child graphs.

For more details see [GraphRegistry API docs](https://winter.jentz.io/javadoc/winter/io.jentz.winter/-graph-registry/index.html)

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

