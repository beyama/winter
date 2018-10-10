package io.jentz.winter.rxjava2

import io.jentz.winter.WinterPlugins
import io.jentz.winter.graph
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WinterDisposablePluginTest {

    @BeforeEach
    fun beforeEach() {
        WinterDisposablePlugin.install()
    }

    @AfterEach
    fun afterEach() {
        WinterDisposablePlugin.uninstall()
    }

    @Test
    fun `should install and uninstall plugin`() {
        graph { }.instanceOrNull<CompositeDisposable>().shouldBeInstanceOf<CompositeDisposable>()

        WinterDisposablePlugin.uninstall()

        graph { }.instanceOrNull<CompositeDisposable>().shouldBe(null)
    }

    @Test
    fun `should dispose CompositeDisposable when graph gets disposed`() {
        graph {}.apply {
            val disposable: CompositeDisposable = instance()
            disposable.isDisposed.shouldBeFalse()
            dispose()
            disposable.isDisposed.shouldBeTrue()
        }
    }

    @Test
    fun `should add all disposable singletons to CompositeDisposable`() {
        val d1 = CompositeDisposable()
        val d2 = CompositeDisposable()
        graph {
            singleton<Disposable>("d1") { d1 }
            prototype<Disposable>("d2") { d2 }
        }.apply {
            val disposables: CompositeDisposable = instance()
            instancesOfType<Disposable>().shouldHaveSize(2)
            disposables.size().shouldBe(1)
            disposables.remove(d1).shouldBeTrue()
            disposables.remove(d2).shouldBeFalse()
        }
    }

}