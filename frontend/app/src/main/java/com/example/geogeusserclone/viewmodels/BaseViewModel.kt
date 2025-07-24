package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<T>(initialState: T) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<T> = _state.asStateFlow()

    protected fun setState(newState: T) {
        _state.value = newState
    }

    protected fun updateState(transform: (T) -> T) {
        _state.value = transform(_state.value)
    }
}
