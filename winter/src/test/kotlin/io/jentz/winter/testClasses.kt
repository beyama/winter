package io.jentz.winter

interface Pump

class Heater

class Thermosiphon(val heater: Heater) : Pump

class CoffeeMaker(val heater: Heater, val pump: Pump)

class Parent(val child: Child)

class Child {
    var parent: Parent? = null
}

open class Service {
    var property = 0
}

class ExtendedService : Service()
