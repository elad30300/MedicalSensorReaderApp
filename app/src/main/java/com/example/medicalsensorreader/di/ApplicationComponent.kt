package com.example.medicalsensorreader.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    ApplicationSubcomponentsModule::class
])
interface ApplicationComponent {

//    @Component.Builder
//    interface Builder {
//        @BindsInstance
//        fun application(application: Application): Builder
//
//        fun build(): ApplicationComponent
//    }

    fun mainActivityComponent() : MainActivityComponent.Factory
}