package io.jentz.winter.androidx.lifecyle

import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever

fun mockLifecycleOwner() =
    mock<LifecycleOwner>().also { whenever(it.lifecycle).thenReturn(mock()) }