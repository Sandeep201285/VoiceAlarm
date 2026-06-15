package com.echocare.app.di

import android.content.Context
import androidx.room.Room
import com.echocare.app.data.local.EchoCareDatabase
import com.echocare.app.data.local.dao.GroupDao
import com.echocare.app.data.local.dao.GroupMemberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): EchoCareDatabase {
        // Phase 1: simple hardcoded passphrase.
        // Phase 2+: derive from user biometric / keystore.
        val passphrase: ByteArray = SQLiteDatabase.getBytes("echocare-p1-passphrase".toCharArray())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            EchoCareDatabase::class.java,
            "echocare.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReminderDao(db: EchoCareDatabase) = db.reminderDao()

    @Provides
    fun provideDeliveryLogDao(db: EchoCareDatabase) = db.deliveryLogDao()

    @Provides
    @Singleton
    fun provideGroupDao(db: EchoCareDatabase): GroupDao = db.groupDao()

    @Provides
    @Singleton
    fun provideGroupMemberDao(db: EchoCareDatabase): GroupMemberDao = db.groupMemberDao()
}
