package org.qosp.notes.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.qosp.notes.data.AppDatabase
import org.qosp.notes.data.repo.*
import org.qosp.notes.data.sync.core.SyncManager
import org.qosp.notes.preferences.PreferenceRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNotebookRepository(
        appDatabase: AppDatabase,
        noteRepository: NoteRepository,
        syncManager: SyncManager,
    ) = NotebookRepository(appDatabase.notebookDao, noteRepository, syncManager)

    @Provides
    @Named("NO_SYNC")
    @Singleton
    fun provideNotebookRepositoryWithNullSyncManager(
        appDatabase: AppDatabase,
        @Named("NO_SYNC") noteRepository: NoteRepository,
    ) = NotebookRepository(appDatabase.notebookDao, noteRepository, null)

    @Provides
    @Singleton
    fun provideNoteRepository(
        appDatabase: AppDatabase,
        syncManager: SyncManager,
    ) = NoteRepository(appDatabase.noteDao, appDatabase.idMappingDao, syncManager)

    @Provides
    @Named("NO_SYNC")
    @Singleton
    fun provideNoteRepositoryWithNullSyncManager(
        appDatabase: AppDatabase,
    ) = NoteRepository(appDatabase.noteDao, appDatabase.idMappingDao, null)

    @Provides
    @Singleton
    fun provideReminderRepository(appDatabase: AppDatabase) = ReminderRepository(appDatabase.reminderDao)

    @Provides
    @Singleton
    fun provideTagRepository(
        appDatabase: AppDatabase,
        syncManager: SyncManager,
        noteRepository: NoteRepository,
    ) = TagRepository(appDatabase.tagDao, appDatabase.noteTagDao, noteRepository, syncManager)

    @Provides
    @Singleton
    fun provideCloudIdRepository(appDatabase: AppDatabase) = IdMappingRepository(appDatabase.idMappingDao)

    @Provides
    @Singleton
    fun providePreferenceRepository(@ApplicationContext context: Context) = PreferenceRepository(context)
}