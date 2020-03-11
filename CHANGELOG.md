Changelog
=========

Version 0.8.0
-------------
* Add support for generated components
* Add compiler support for Function0 types
* Add services for set and maps of type
* Remove fragment factory support
* Remove *ofType methods from Graph and property delegates

Version 0.7.1
-------------
* Add consumer ProGuard file to winter-androidx module
* Add originating element to type spec in winter-compiler 

Version 0.7.0
-------------

* New WinterTestSession class for less boilerplate code in tests
* New JUnit4 WinterRule that utilizes WinterTestSession
* New JUnit5 WinterEachExtension that utilizes WinterTestSession
* New JUnit5 WinterAllExtension that utilizes WinterTestSession
* New ApplicationScope, ActivityScope & PresentationScope annotations
* New InjectConstructor, Prototype & EagerSingleton annotations
* Winter compiler supports incremental annotation processing (isolating)
* Winter compiler generates Java instead of Kotlin code  
* ComponentBuilder is now Component.Builder
* Simplify injection adapter and add WinterApplication#inject method
* Remove support for factories in Components/Graphs (factory/multiton)
* Remove support for generated components from Winter compiler
* Remove WinterTree and GraphRegistry
* Remove WinterInjection and Injection
* Remove support for generated components
* Remove WinterApplication #registerPlugin & #unregisterPlugin
* Remove GraphLifecycleTestRule in favour of WinterRule
* Remove ExtendGraphTestRule in favour of WinterRule
* Remove ExtendGraphExtension in favour of WinterEachExtension
* Remove GraphLifecycleExtension in favour of WinterEachExtension
* Remove WinterJUnit4 and WinterJUnit5 classes
* Remove injectSuperClasses argument from all JSR330 inject methods because it is obsolete
* Make ApplicationScope::class the default component qualifier
* Use new scope annotations in AndroidX adapters
* Validate that a component qualifier is unique in the component tree
* Validate that an included component has the same qualifier
* Validate that a factory scope annotation is the same as the component qualifier when registering generated factories
* WinterApplication#component will throw an exception if the application graph is already open
* Component qualifier is now mandatory
* Add call to superclass members injector to members injector
* Retrieve members injector with Class.forName instead of registering them on the generated component

Version 0.6.0 *2019-11-24*
--------------------------

* Remove all deprecated methods
* Add winter-java module (JWinter) for easy dependency retrieval from Java
* Add generic arguments to TypeKey and the methods consuming the key


Version 0.5.0
-------------

* Add option to enable cyclic dependency checks (disabled by default)
* Restructure Graph and Injector to reduce synchronized calls
* Add configuration dependant ServiceEvaluators to optimize performance 

Version 0.4.0
-------------

* Add winter-junit5 support module
* Add ability to provide all Mock and Spy annotated fields in tests via a graph
* Add Plugin#graphInitialized callback
* Remove exception in ComponentBuilder#register when override is true but service does not exist
* Rename Plugin#initializingComponent to #graphInitializing
* Deprecate WinterTestRule in favor of ExtendGraphTestRule 

Version 0.3.1
-------------

* New #createSubgraph, #openSubgraph, #closeSubgraph
* New Component#createGraph method
* New WinterApplication#createGraph
* Deprecate #createChildGraph, #openChildGraph, #closeChildGraph methods
* Deprecate Component#init
* Deprecate WinterApplication#init 
* Update documentation and exception messages to reflect new naming

Version 0.3.0
-------------

* New openGraph/closeGraph methods in Graph to manage child graphs as services
* New Graph#createChildGraph method in graph to replace now deprecated Graph#initializeSubcomponent
  method to have a more consistent naming with WinterTree (#open, #close, #create)
* New Android X Lifecycle extension for auto-dispose on lifecycle event
* New WinterAware interface to add Winter extension functions to any class that requires them
* New winterRootScopeAnnotation compiler argument to configure a custom scope annotation as root
  scope for the generated component
* Deprecate Android View extensions and ComponentCallback2 extensions in favour of WinterAware
* Internal restructuring of Winter compiler for better testing
* Remove winterPrintSources compiler option
* Compiler doesn't generate factory classes anymore
* Update all dependencies to latest stable versions

Version 0.2.0
-------------

* New WinterApplication base class for registering application component and plugins
* New Winter object as default WinterApplication
* Plugins are now registered on WinterApplication objects and not global anymore
* New Plugin interface
* New WinterInjection base class for use in libraries 
* New Injection class in core (extracted from AndroidInjection)
* New Injection#createGraph now takes an optional component builder block
* Deprecates AndroidInjection in favour of core Injection
* New ComponentCallbacks2 extension methods
* New View extension methods
* New RxJava2 module with WinterDisposablePlugin
* New Winter JUnit 4 module for easier testing
* Fix: Make singleton, factory & multiton scopes thread safe
* Breaking: View.graph extension renamed to dependencyGraph

Version 0.1.0
-------------

* Init complete and dispose callbacks for factories
* Type aliases
* API to allow custom scopes
* Post-Construct-Plugins run also for factories with argument
* Resolver methods that allow to supply an argument for a factory
* Rewrite of the internal structure of Component and Graph entries
* Use JUnit 5 and KotlinTest matcher


Version 0.0.4 *(2018-07-13)*
----------------------------

 * Add component and parentGraph property to Graph
 * Add optional qualifier on Component (useful in Winter plugins)