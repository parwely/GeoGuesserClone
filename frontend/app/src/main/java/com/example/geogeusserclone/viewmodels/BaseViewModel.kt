package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<T>(initialState: T) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<T> = _state

    protected fun setState(newState: T) {
        _state.value = newState
    }
}