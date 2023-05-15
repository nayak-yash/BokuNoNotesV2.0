package com.bokuno.notes.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    var title:String?=null,
    var text:String?=null,
    var createdAt:Long?=null,
    val userId:String?=null,
    @PrimaryKey(autoGenerate = true)
    val noteId:Long?=null,
    var location:String?=null,
    var status:Boolean?=null,              /* true -> done,  false -> todo , null -> default */
    @field:JvmField // use this annotation if your Boolean field is prefixed with 'is'
    var isFavorite:Boolean=false,
    @field:JvmField
    var isPrivate:Boolean=false
)