package io.jentz.winter.junit4

import io.jentz.winter.graph
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named

class GraphExtTest {

    @Inject
    @Named("one")
    private var privateNamedVar: Int? = null

    @field:[Inject Named("two")]
    private var privateNamedVarWithFieldAnnotation: Int? = null

    @set:Inject
    private var getterAndSetterWithBackingField: String?
        get() = backingField
        set(value) {
            backingField = value
        }

    private var backingField: String? = null

    @Inject
    lateinit var lateinitProperty: String

    private val graph = graph {
        prototype("one") { 1 }
        prototype("two") { 2 }
        prototype { "test" }
    }

    @Before
    fun beforeEach() {
        privateNamedVar = null
        privateNamedVarWithFieldAnnotation = null
        backingField = null
        lateinitProperty = ""
    }

    @Test
    fun `#injectWithReflection should inject into named private var`() {
        graph.injectWithReflection(this)
        privateNamedVar.shouldBe(1)
        privateNamedVarWithFieldAnnotation.shouldBe(2)
    }

    @Test
    fun `#injectWithReflection should inject into setter`() {
        graph.injectWithReflection(this)
        backingField.shouldBe("test")
    }

    @Test
    fun `#injectWithReflection should inject into lateinit property`() {
        graph.injectWithReflection(this)
        lateinitProperty.shouldBe("test")
    }

}