package io.jentz.winter.testing

import io.jentz.winter.graph
import io.jentz.winter.qualifier
import io.jentz.winter.typeKey
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        prototype(typeKey(qualifier("one"))) { 1 }
        prototype(typeKey(qualifier("two"))) { 2 }
        prototype { "test" }
    }

    @BeforeEach
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