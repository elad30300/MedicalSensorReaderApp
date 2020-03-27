package com.example.medicalsensorreader.di

import com.example.medicalsensorreader.MainActivity
import dagger.Subcomponent

@MainActivityScope
@Subcomponent
interface MainActivityComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): MainActivityComponent
    }

    fun inject(mainActivity: MainActivity)
}