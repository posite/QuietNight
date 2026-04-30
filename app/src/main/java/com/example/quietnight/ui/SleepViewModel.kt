package com.example.quietnight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SleepViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

}
