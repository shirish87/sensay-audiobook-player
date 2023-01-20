package com.dotslashlabs.sensay.ui.screen.lookup

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.HomeViewModel
import com.dotslashlabs.sensay.ui.screen.home.HomeViewState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.BookWithChapters
import lookup.BookLookup
import lookup.Item


data class LookupViewState(
    @PersistState val bookId: Long,
    val bookWithChapters: Async<BookWithChapters> = Uninitialized,
    val searchResultItems: Async<List<Item>> = Uninitialized,
) : MavericksState {

    constructor(args: LookupViewArgs) : this(bookId = args.bookId)
}


class LookupViewModel @AssistedInject constructor(
    @Assisted state: LookupViewState,
    store: SensayStore,
    private val bookLookup: BookLookup,
) : MavericksViewModel<LookupViewState>(state) {

    init {
        val bookId = state.bookId

        store.bookWithChapters(bookId)
            .execute(retainValue = LookupViewState::bookWithChapters) {
                copy(bookWithChapters = it)
            }

        onAsync(LookupViewState::bookWithChapters) { book ->
            setState { copy(searchResultItems = Loading()) }

            try {
                val items = bookLookup.lookup(book.book.title, book.book.author, book.book.series)
                setState { copy(searchResultItems = Success(items)) }
            } catch (t: Throwable) {
                setState { copy(searchResultItems = Fail(t)) }
            }
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<LookupViewModel, LookupViewState> {
        override fun create(state: LookupViewState): LookupViewModel
    }

    companion object : MavericksViewModelFactory<LookupViewModel, LookupViewState>
    by hiltMavericksViewModelFactory()
}

data class LookupViewArgs(
    val bookId: Long,
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

    companion object CREATOR : Parcelable.Creator<LookupViewArgs> {
        override fun createFromParcel(parcel: Parcel): LookupViewArgs {
            return LookupViewArgs(parcel)
        }

        override fun newArray(size: Int): Array<LookupViewArgs?> {
            return arrayOfNulls(size)
        }
    }
}
