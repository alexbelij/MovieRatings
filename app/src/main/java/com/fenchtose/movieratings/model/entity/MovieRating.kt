package com.fenchtose.movieratings.model.entity

import com.fenchtose.movieratings.util.Constants
import com.fenchtose.movieratings.util.FixTitleUtils
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val MOVIE_ID_404 = "404movie"

@JsonClass(generateAdapter = true)
data class MovieRating(
        @Json(name="id")
        val imdbId: String,

        @Json(name="rating")
        val rating: Float,

        @Json(name="votes")
        val votes: Int,

        @Json(name="title")
        val title: String,

        @Json(name="type")
        val type: String,

        @Json(name="source")
        val source: String,

        @Json(name = "translated_title")
        val translatedTitle: String,

        @Json(name="start_year")
        val startYear: Int,

        @Json(name="end_year")
        val endYear: Int? = -1) {

    fun convert(timestamp: Int): com.fenchtose.movieratings.model.db.entity.MovieRating {
        return com.fenchtose.movieratings.model.db.entity.MovieRating(
                imdbId = imdbId,
                rating = rating,
                votes = votes,
                title = title,
                type = type,
                translatedTitle = translatedTitle,
                startYear = startYear,
                endYear = endYear?: -1,
                timestamp = timestamp,
                source = source
        )
    }

    fun displayRating(): String {
        return when {
            is404() -> "Not found. Search on Imdb?"
            isImdb() -> "%.1f".format(rating)
            else -> "%.1f (%s)".format(rating, source)
        }
    }

    fun displayYear(): String {
        if (startYear <= 0) {
            return ""
        }

        if (endYear == null || endYear <= 0) {
            if (type == Constants.RATING_TYPE_SERIES) {
                return "($startYear - )"
            } else {
                return "($startYear)"
            }
        }

        return "($startYear - $endYear)"
    }

    fun is404() = imdbId == MOVIE_ID_404
    fun isImdb() = source == "IMDB"

    companion object {
        fun empty(): MovieRating {
            return MovieRating("", -1f, -1, "", "", "", "",-1, -1)
        }

        fun create404Dummy(title: String): MovieRating {
            return MovieRating(MOVIE_ID_404, -1f, -1, title, "", "", "", -1, -1)
        }

        fun fromMovie(movie: OmdbMovie): MovieRating {

            movie.ratings.firstOrNull {
                it.source == "Internet Movie Database"
            }?.let {
                var startYear = -1
                var endYear = -1

                val years = FixTitleUtils.splitYears(movie.year)
                if (years.isNotEmpty()) {
                    startYear = years[0].toIntOrNull() ?: -1
                }
                if (years.size > 1) {
                    endYear = years[1].toIntOrNull() ?: -1
                }

                val rating = MovieRating(
                        imdbId = movie.imdbId,
                        type = movie.type,
                        title = movie.title,
                        rating = it.rating.split("/").firstOrNull()?.toFloatOrNull() ?:0f,
                        votes = movie.imdbVotes.replace(",","").toIntOrNull() ?: -1,
                        startYear = startYear,
                        endYear = endYear,
                        translatedTitle = "",
                        source = "IMDB"
                )

                return rating
            }

            return empty()
        }
    }
}

fun com.fenchtose.movieratings.model.db.entity.MovieRating.convert(): MovieRating {
    return MovieRating(
            imdbId = imdbId,
            rating = rating,
            votes = votes,
            title = title,
            type = type,
            translatedTitle = translatedTitle,
            startYear = startYear,
            endYear = endYear,
            source = source
    )
}