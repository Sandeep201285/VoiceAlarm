package com.echocare.app.di

// AppModule is intentionally minimal in Phase 1.
// AlarmScheduler and NotificationHelper are self-provided via
// @Singleton + @Inject constructor(@ApplicationContext ...) — no @Provides needed.
// Phase 3 will expand this module with network/cloud bindings.

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule
