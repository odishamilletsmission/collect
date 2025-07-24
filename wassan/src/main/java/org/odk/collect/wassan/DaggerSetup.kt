package org.odk.collect.wassan

import dagger.Component
import dagger.Module
import dagger.Provides
import org.odk.collect.async.Scheduler
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.wassan.activity.MainActivity
import org.odk.collect.wassan.activity.SplashActivity

import javax.inject.Singleton

interface WassanDependencyComponentProvider {
    val wassanDependencyComponent: WassanDependencyComponent
}

@Component(modules = [WassanDependencyModule::class])
@Singleton
interface WassanDependencyComponent {

    @Component.Builder
    interface Builder {

        fun wassanDependencyModule(wassanDependencyModule: WassanDependencyModule): Builder

        fun build(): WassanDependencyComponent
    }

    fun inject(splashActivity: SplashActivity)
    fun inject(mainActivity: MainActivity)
}

@Module
open class WassanDependencyModule {

    @Provides
    open fun providesProjectRepository(): ProjectsRepository {
        throw UnsupportedOperationException("This should be overridden by dependent application")
    }

    @Provides
    open fun providesScheduler(): Scheduler {
        throw UnsupportedOperationException("This should be overridden by dependent application")
    }
}
