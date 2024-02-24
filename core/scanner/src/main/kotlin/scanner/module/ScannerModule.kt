package scanner.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import scanner.CoverScanner
import scanner.MediaScanner
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ScannerModule {

    @Provides
    @Singleton
    fun provideMediaScanner() = MediaScanner()

    @Provides
    @Singleton
    fun provideCoverScanner() = CoverScanner()
}
