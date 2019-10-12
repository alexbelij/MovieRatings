package com.fenchtose.movieratings.model.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "COLLECTIONS")
class MovieCollection {
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    @ColumnInfo(name = "COLLECTION_ID")
    var id: Long = 0

    @SerializedName("name")
    @ColumnInfo(name = "COLLECTION_NAME")
    var name: String = ""

    @Transient
    @ColumnInfo(name = "CREATED_AT")
    var createdAt: Long = 0

    @Transient
    @ColumnInfo(name = "UPDATED_AT")
    var updatedAt: Long = 0

    @Transient
    @ColumnInfo(name = "IS_DELETED")
    var deleted: Int = 0

    @SerializedName("entries")
    @Ignore
    var entries: List<MovieCollectionEntry> = ArrayList()

    @Transient
    @Ignore
    var movies: List<Movie> = ArrayList()

    companion object {
        fun create(name: String): MovieCollection {
            val collection = MovieCollection()
            collection.name = name
            collection.createdAt = System.currentTimeMillis()
            collection.updatedAt = collection.createdAt
            return collection
        }

        fun invalid(): MovieCollection {
            val collection = MovieCollection()
            collection.name = ""
            collection.id = -1
            return collection
        }
    }
}