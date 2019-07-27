package io.jentz.winter.androidx.lifecyle

import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock

fun mockLifecycleOwner(): LifecycleOwner = mock { on(it.lifecycle).doReturn(mock()) }