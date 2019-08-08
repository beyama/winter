Changelog
=========

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