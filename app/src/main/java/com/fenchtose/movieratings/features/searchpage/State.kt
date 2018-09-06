package com.fenchtose.movieratings.features.searchpage

import com.fenchtose.movieratings.MovieRatingsApplication
import com.fenchtose.movieratings.base.AppState
import com.fenchtose.movieratings.base.redux.*
import com.fenchtose.movieratings.features.moviecollection.collectionpage.MovieCollectionOp
import com.fenchtose.movieratings.model.api.provider.MovieProvider
import com.fenchtose.movieratings.model.db.like.DbLikeStore
import com.fenchtose.movieratings.model.db.like.LikeStore
import com.fenchtose.movieratings.model.db.like.MovieLiked
import com.fenchtose.movieratings.model.entity.Movie
import com.fenchtose.movieratings.model.entity.MovieCollection
import com.fenchtose.movieratings.model.entity.hasMovie
import com.fenchtose.movieratings.model.entity.updateMovie
import com.fenchtose.movieratings.util.addAll
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.Collections

data class SearchPageState(
        val query: String = "",
        val progress: Progress = Progress.Default,
        val movies: List<Movie> = Collections.emptyList(),
        val page: Int = 0) {
    override fun toString(): String {
        return "SearchPageState(query='$query', progress=$progress, movies=${movies.size}, page=$page)"
    }
}

data class CollectionSearchPageState(
        val searchPageState: SearchPageState = SearchPageState(),
        val collection: MovieCollection = MovieCollection.invalid(),
        val collectionOp: MovieCollectionOp? = null
        )

sealed class Progress {
    object NoOp: Progress()
    object Default: Progress()
    data class Loading(val query: String): Progress()
    object Error: Progress()

    sealed class Success(val movies: List<Movie>, val page: Int): Progress() {
        class Loaded(movies: List<Movie>, page: Int): Success(movies, page)
        class Pagination(movies: List<Movie>, page: Int): Success(movies, page)
    }

    object Paginating: Progress()
    object PaginationError: Progress()
}

sealed class SearchAction(val addToCollection: Boolean): Action {
    class ClearSearch(addToCollection: Boolean): SearchAction(addToCollection)
    class Search(val query: String, addToCollection: Boolean): SearchAction(addToCollection)
    class LoadMore(addToCollection: Boolean): SearchAction(addToCollection)
    class Result(val progress: Progress, addToCollection: Boolean): SearchAction(addToCollection)
    class Reload(val query: String, addToCollection: Boolean): SearchAction(addToCollection)
}

data class InitCollectionSearchPage(val collection: MovieCollection): Action
object ClearCollectionSearchPage: Action
object ClearCollectionOp: Action


fun searchPageReducer(state: AppState, action: Action): AppState {
    return when(action) {
        is SearchAction ->
            if (!action.addToCollection) {
                reduceChildState(state, state.searchPage, action, ::reduce, { s, c -> s.copy(searchPage = c) })
            } else {
                reduceChildState(state, state.collectionSearchPage, action, ::reduce, { s, c -> s.copy(collectionSearchPage = c)})
            }
        is InitCollectionSearchPage, is ClearCollectionSearchPage, is ClearCollectionOp -> reduceChildState(state, state.collectionSearchPage, action, ::reduce, { s, c -> s.copy(collectionSearchPage = c)})
        else -> state
    }
}

private fun reduce(state: SearchPageState, action: Action): SearchPageState {
    return when(action) {
        is SearchAction -> {
            when(action) {
                is SearchAction.Search -> state // NO-OP
                is SearchAction.Result -> {
                    val progress = action.progress
                    when(progress) {
                        is Progress.Loading -> state.copy(query = progress.query, progress = progress)
                        is Progress.Success.Loaded -> state.copy(progress = progress, movies = progress.movies, page = progress.page)
                        is Progress.Success.Pagination -> state.copy(progress = progress, movies = state.movies.addAll(progress.movies), page = progress.page)
                        else -> state.copy(progress = progress)
                    }
                }
                is SearchAction.ClearSearch -> SearchPageState()
                is SearchAction.LoadMore -> state // NO-OP
                is SearchAction.Reload -> state // NO-OP
            }
        }
        is MovieLiked -> {
            if (state.movies.hasMovie(action.movie) != -1) {
                   state.copy(movies = state.movies.updateMovie(action.movie))
            } else {
                state
            }
        }
        else -> state
    }
}

private fun reduce(state: CollectionSearchPageState, action: Action): CollectionSearchPageState {
    return when(action) {
        is SearchAction -> reduceChildState(state, state.searchPageState, action, ::reduce, {s, c -> s.copy(searchPageState = c)})
        is InitCollectionSearchPage -> state.copy(searchPageState = SearchPageState(), collection = action.collection)
        is ClearCollectionSearchPage -> CollectionSearchPageState()
        is ClearCollectionOp -> state.copy(collectionOp = null)
        else -> state
    }
}

class SearchMiddleWare(private val provider: MovieProvider,
                       likeStore: LikeStore) {

    init {
        provider.addPreferenceApplier(likeStore)
    }

    fun searchMiddleware(state: AppState, action: Action, dispatch: Dispatch, next: Next<AppState>): Action {
        return when(action) {
            is SearchAction.Search -> {
                if (action.addToCollection) {
                    if (state.collectionSearchPage.searchPageState.query == action.query && state.collectionSearchPage.searchPageState.movies.isNotEmpty()) {
                        NoAction
                    } else {
                        makeApiCall(action.query, 1, true, dispatch)
                        SearchAction.Result(Progress.Loading(action.query), true)
                    }
                } else {
                    if (state.searchPage.query == action.query && state.searchPage.movies.isNotEmpty()) {
                        NoAction
                    } else {
                        makeApiCall(action.query, 1, false, dispatch)
                        SearchAction.Result(Progress.Loading(action.query), false)
                    }
                }
            }

            is SearchAction.LoadMore -> {
                makeApiCall(state.searchPage.query, state.searchPage.page + 1, action.addToCollection, dispatch)
                SearchAction.Result(Progress.Paginating, action.addToCollection)
            }

            is SearchAction.Reload -> {
                if (action.query.isBlank()) {
                    NoAction
                } else {
                    makeApiCall(action.query, 1, action.addToCollection, dispatch)
                    SearchAction.Result(Progress.Loading(action.query), action.addToCollection)
                }
            }

            else -> next(state, action, dispatch)
        }
    }

    private fun makeApiCall(query: String, page: Int, addToCollection: Boolean, dispatch: Dispatch) {
        provider.search(query, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    if (it.error != null) {
                        dispatch(SearchAction.Result(if (page == 1) Progress.Error else Progress.PaginationError, addToCollection))
                    } else {
                        dispatch(SearchAction.Result(if (page == 1) Progress.Success.Loaded(it.movies, page) else Progress.Success.Pagination(it.movies, page), addToCollection))
                    }
                }, {
                    it.printStackTrace()
                    dispatch(SearchAction.Result(if (page == 1) Progress.Error else Progress.Paginating, addToCollection))
                })
    }

    companion object {
        fun newInstance(): SearchMiddleWare {
            return SearchMiddleWare(
                    MovieRatingsApplication.movieProviderModule.movieProvider,
                    DbLikeStore.getInstance(MovieRatingsApplication.database.favDao()))
        }
    }
}

