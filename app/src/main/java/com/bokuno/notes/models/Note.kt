package com.bokuno.notes.models

data class Note(
    val title:String?=null,
    val text:String?=null,
    val createdAt:Long?=null,
    val userId:String?=null,
    val location:String?=null,
    val status:Boolean?=null,              /* true -> done,  false -> todo , null -> default */
    @field:JvmField // use this annotation if your Boolean field is prefixed with 'is'
    val isFavorite:Boolean=false,
    @field:JvmField
    val isPrivate:Boolean=false
)