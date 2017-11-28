# Winter

[![Kotlin 1.1.51](https://img.shields.io/badge/Kotlin-1.1-blue.svg)](http://kotlinlang.org)
[![Nexus](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.jentz/winter.svg)](https://oss.sonatype.org/content/repositories/snapshots/io/jentz/)
[![Travis](https://img.shields.io/travis/beyama/winter.svg)](https://travis-ci.org/beyama/winter/builds)
[![MIT License](https://img.shields.io/github/license/beyama/winter.svg)](https://github.com/beyama/winter/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/beyama/winter.svg)](https://github.com/beyama/winter/issues)

Kotlin Dependency Injection
===========================

Winter is a fast and intuitive dependency injection and service locator
library for Kotlin on Android and the JVM.

It offers an idiomatic Kotlin API as well as optional JSR-330 support
with annotation processor.

Basic Concepts
--------------

The building blocks of Winter are:

| Term      | Definition                                                                                                                             |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------|
| Component | The immutable dependency registry for providers, factories and subcomponents.                                                          |
| Graph     | The "instance" of a component that holds actual instances and is used to retrieve dependencies defined in a component.                 |
| Scope     | The lifetime of an instance in a graph e.g. singleton for only one per graph or prototype for one each time an instance is requested.  |

Simple Example
--------------

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

Injector
--------

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
    private val factory: (Int) -> ProducedInstance by injector.lazyFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
      // ... create or get the dependency graph
      injector.inject(graph)
      super.onCreate(savedInstanceState)
    }
}

Lazy injection means that the actual retrieval and therefore the actual
instantiation of a dependency is deferred to the point where you access
the property the first time. This is useful in cases where the creation
is computationally expensive but may not be required in some cases.

```

JSR-330 Annotation Processor
----------------------------

JSR-330 support is provided by the Winter module `winter-compiler`.

Winter generates factories for your classes that have a @Inject
annotated constructor.

It generates a members-injector for each class that has @Inject annotated
setter or fields.

And it generates a component containing all those factories and
members-injectors to avoid the usage of reflection.

Android Support
---------------

*TODO:* AndroidInjection, view-extensions

Advanced Usage
--------------

*TODO:* mixing components, usage of factories, type erasure, qualifier

License
-------

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

