# Winter

[![CI](https://github.com/beyama/winter/actions/workflows/ci.yml/badge.svg)](https://github.com/beyama/winter/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.jentz.winter/winter.svg)](https://central.sonatype.com/namespace/io.jentz.winter)
[![License](https://img.shields.io/github/license/beyama/winter.svg)](LICENSE)

Winter is a small and fast dependency injection library for Kotlin Multiplatform: JVM, Android,
iOS, macOS, JavaScript and WebAssembly.

- Dependencies are declared in immutable components with a plain Kotlin DSL. No annotation
  processing and no code generation.
- Components nest: subcomponents describe parts of an application with a shorter lifetime, like a
  user session or an Android activity.
- Components can be composed from parts of different modules or targets: include, merge, derive
  and override them.
- Optional modules for property injection, Android lifecycles and JUnit 4/5 tests.

**[Guide](https://winter.jentz.io)** · **[API docs](https://winter.jentz.io/api/)**

## Installation

```kotlin
dependencies {
    implementation("io.jentz.winter:winter:0.10.0-alpha1")
    // optional
    implementation("io.jentz.winter:winter-delegate:0.10.0-alpha1")
    implementation("io.jentz.winter:winter-androidx:0.10.0-alpha1")
    testImplementation("io.jentz.winter:winter-junit4:0.10.0-alpha1")
    testImplementation("io.jentz.winter:winter-junit5:0.10.0-alpha1")
}
```

0.10.0-alpha1 is the first release of the reworked API, see the [changelog](CHANGELOG.md).
Snapshots are published to the Maven Central snapshot repository:

```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}
```

Winter requires Java 17 and Kotlin 1.9 or newer, the AndroidX module requires minSdk 24.

The core and delegate modules are Kotlin Multiplatform libraries (JVM, `iosArm64`,
`iosSimulatorArm64`, `iosX64`, `macosArm64`, `js`, `wasmJs`), add them to `commonMain`. Native,
JavaScript and WebAssembly projects need Kotlin 2.4 or newer. The AndroidX and JUnit modules are
Android and JVM only. See [Multiplatform](https://winter.jentz.io/#_multiplatform) in the guide for
the differences between the platforms.

## Example

```kotlin
val coffeeComponent = component {
    singleton { Heater() }
    singleton<Pump> { RotaryPump() }
    prototype { CoffeeMaker(instance(), instance()) }
}

val graph = coffeeComponent.createGraph()
val coffeeMaker: CoffeeMaker = graph.instance()
```

On Android, an activity gets its dependencies from an activity subgraph whose parent survives
configuration changes:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Winter.component {
            singletonOf(::GitHubApi)
            activityGraphResolver<RepoListActivity>()

            subcomponent(ViewModelScope) {
                singletonOf(::RepoListViewModel)
                subcomponent(ActivityScope) {
                    singleton { ImageLoader(instance()) }
                }
            }
        }

        Winter.openGraph { application(this@MyApplication) }
    }
}

class RepoListActivity : ComponentActivity() {
    private val injector by Winter
    private val viewModel: RepoListViewModel by injector()

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject()
        super.onCreate(savedInstanceState)
    }
}
```

See the [guide](https://winter.jentz.io) for everything else.

## Development

```bash
./gradlew jvmTest test         # JVM tests
./gradlew allTests             # tests on all targets that the host can run
./gradlew :dokkaGenerate       # API docs in build/dokka/html
doc/build-site.sh              # guide and API docs in build/site (needs asciidoctor)
```

The code samples of the guide (`doc/index.adoc`) live in `docs` test packages of the modules and
are included by tag, so they are compiled and run with the tests.

The iOS simulator tests need an installed iOS simulator runtime, exclude them with
`-x :winter:iosSimulatorArm64Test -x :winter-delegate:iosSimulatorArm64Test` otherwise.

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
