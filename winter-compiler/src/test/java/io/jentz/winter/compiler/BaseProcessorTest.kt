package io.jentz.winter.compiler

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseProcessorTest {

    @BeforeEach
    fun beforeEach() {
        currentDateFixed = ISO8601_FORMAT.parse("2019-02-10T14:52Z")
    }

    @AfterEach
    fun afterEach() {
        currentDateFixed = null
    }

}
