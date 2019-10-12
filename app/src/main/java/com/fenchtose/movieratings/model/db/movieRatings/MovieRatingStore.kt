package com.fenchtose.movieratings.model.db.movieRatings

import androidx.annotation.WorkerThread

interface MovieRatingStore {
    @WorkerThread
    fun update404(title: String, year: String? = null)

    @WorkerThread
    fun was404(title: String, year: String?, timestamp: Long): Boolean
}