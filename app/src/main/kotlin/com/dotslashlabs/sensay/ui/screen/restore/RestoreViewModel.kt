package com.dotslashlabs.sensay.ui.screen.restore

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.BookProgressUpdate
import data.SensayStore
import data.entity.BookId
import data.entity.BookProgressWithBookAndChapters
import data.entity.Progress
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class RestoreViewState(
    @PersistState val bookId: BookId,

    val bookProgressWithBookAndChapters: Async<BookProgressWithBookAndChapters> = Uninitialized,
    val progressRestorable: Async<List<Progress>> = Uninitialized,
) : MavericksState {

    constructor(args: RestoreViewArgs) : this(bookId = args.bookId)
}

class RestoreViewModel @AssistedInject constructor(
    @Assisted private val state: RestoreViewState,
    private val store: SensayStore,
) : MavericksViewModel<RestoreViewState>(state) {

    init {
        val bookId = state.bookId

        store.bookProgressWithBookAndChapters(bookId)
            .execute(retainValue = RestoreViewState::bookProgressWithBookAndChapters) {
                copy(bookProgressWithBookAndChapters = it)
            }

        store.progressRestorable()
            .execute(retainValue = RestoreViewState::progressRestorable) {
                copy(progressRestorable = it)
            }
    }

    fun deleteProgress(progress: Progress) = viewModelScope.launch(Dispatchers.IO) {
        store.deleteProgress(progress)
    }

    fun restoreBookProgress(
        bookProgressWithChapters: BookProgressWithBookAndChapters,
        progress: Progress,
    ) = viewModelScope.launch(Dispatchers.IO) {

        val currentChapter = bookProgressWithChapters.chapters
            .sortedBy { it.trackId }
            .getOrNull(progress.currentChapter - 1) ?: return@launch

        store.updateBookProgress(
            BookProgressUpdate(
                bookProgressId = bookProgressWithChapters.bookProgress.bookProgressId,
                currentChapter = progress.currentChapter,
                chapterProgress = progress.chapterProgress,
                chapterTitle = currentChapter.title,
                bookProgress = progress.bookProgress,
                bookRemaining = ContentDuration.ms(
                    bookProgressWithChapters.book.duration.ms - progress.bookProgress.ms
                ),
                bookCategory = BookCategory.CURRENT,
            )
        )

        store.deleteProgress(progress)
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<RestoreViewModel, RestoreViewState> {
        override fun create(state: RestoreViewState): RestoreViewModel
    }

    companion object : MavericksViewModelFactory<RestoreViewModel, RestoreViewState>
    by hiltMavericksViewModelFactory()
}

data class RestoreViewArgs(
    val bookId: BookId,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
    )

    constructor(bundle: Bundle) : this(bundle.getLong("bookId"))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(bookId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RestoreViewArgs> {
        override fun createFromParcel(parcel: Parcel): RestoreViewArgs {
            return RestoreViewArgs(parcel)
        }

        override fun newArray(size: Int): Array<RestoreViewArgs?> {
            return arrayOfNulls(size)
        }
    }
}
