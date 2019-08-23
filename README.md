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

**[Winter documentation](https://winter.jentz.io/)**

## Installation

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

## Quickstart Android

Given is an Android application with an application component, a presentation subcomponent that 
survives orientation changes and an activity subcomponent.

```kotlin

class MyApplication : Application() {
  override fun onCreate() {
    // define application component
    Winter.component {
      singleton<HttpClient> { HttpClientImpl() }
      singleton<GitHubApi> { GitHubApiImpl(instance()) }
      
        // define presentation subcomponent
        subcomponent("presentation") {
          singleton<ReposListViewModel> { RepolistViewModel(instance<GitHubApi>()) }
          
          // define activity subcomponent
          subcomponent("activity") {
            singleton { Glide.with(instance()) }
          }
        }
    }

    /// Configure Injection to use the Android presentation scope adapter   
    Injection.useAndroidPresentationScopeAdapter()
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

## Read more

Winter is used in production since end of 2017. The API is considered mostly stable but there will
be no guarantee before version 1.x.

Winter supports only Android and JVM right now but support for native (iOS) and JavaScript is planed.  

**[Winter documentation](https://winter.jentz.io/)**

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

