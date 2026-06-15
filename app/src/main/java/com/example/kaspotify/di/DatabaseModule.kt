package com.example.kaspotify.di

import android.content.Context
import androidx.room.Room
import com.example.kaspotify.data.local.MusicDao
import com.example.kaspotify.data.local.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(context, MusicDatabase::class.java, "kaspotify.db")
            .addMigrations(MusicDatabase.MIGRATION_1_2, MusicDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMusicDao(database: MusicDatabase): MusicDao = database.musicDao()
}
