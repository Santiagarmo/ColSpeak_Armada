package com.example.speak

import android.os.Parcel
import android.os.Parcelable

class ImageQuestion : Parcelable {
    val question: String?
    val correctAnswer: String?
    val options: Array<String?>?
    val topic: String?
    val level: String?
    val imageResourceName: String?
    var correctImageResource: String?

    constructor(
        question: String?,
        correctAnswer: String?,
        options: Array<String?>?,
        topic: String?,
        level: String?,
        imageResourceName: String?
    ) {
        this.question = question
        this.correctAnswer = correctAnswer
        this.options = options
        this.topic = topic
        this.level = level
        this.imageResourceName = imageResourceName
        this.correctImageResource = imageResourceName
    }

    protected constructor(`in`: Parcel) {
        question = `in`.readString()
        correctAnswer = `in`.readString()
        options = `in`.createStringArray()
        topic = `in`.readString()
        level = `in`.readString()
        imageResourceName = `in`.readString()
        correctImageResource = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(question)
        dest.writeString(correctAnswer)
        dest.writeStringArray(options)
        dest.writeString(topic)
        dest.writeString(level)
        dest.writeString(imageResourceName)
        dest.writeString(correctImageResource)
    }

    companion object {
        val CREATOR: Parcelable.Creator<ImageQuestion?> =
            object : Parcelable.Creator<ImageQuestion?> {
                override fun createFromParcel(`in`: Parcel): ImageQuestion {
                    return ImageQuestion(`in`)
                }

                override fun newArray(size: Int): Array<ImageQuestion?> {
                    return arrayOfNulls<ImageQuestion>(size)
                }
            }
    }
}