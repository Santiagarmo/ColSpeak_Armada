package com.example.speak

import android.os.Parcel
import android.os.Parcelable

class ListeningQuestion : Parcelable {
    val question: String?
    val correctAnswer: String?
    val options: Array<String?>?
    val topic: String?
    val level: String?
    val isShowText: Boolean
    val info: String?

    constructor(
        question: String?,
        correctAnswer: String?,
        options: Array<String?>?,
        topic: String?,
        level: String?,
        showText: Boolean,
        info: String?
    ) {
        this.question = question
        this.correctAnswer = correctAnswer
        this.options = options
        this.topic = topic
        this.level = level
        this.isShowText = showText
        this.info = info
    }

    protected constructor(`in`: Parcel) {
        question = `in`.readString()
        correctAnswer = `in`.readString()
        options = `in`.createStringArray()
        topic = `in`.readString()
        level = `in`.readString()
        this.isShowText = `in`.readByte().toInt() != 0
        info = `in`.readString()
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
        dest.writeByte((if (this.isShowText) 1 else 0).toByte())
        dest.writeString(info)
    }

    companion object {
        val CREATOR: Parcelable.Creator<ListeningQuestion?> =
            object : Parcelable.Creator<ListeningQuestion?> {
                override fun createFromParcel(`in`: Parcel): ListeningQuestion {
                    return ListeningQuestion(`in`)
                }

                override fun newArray(size: Int): Array<ListeningQuestion?> {
                    return arrayOfNulls<ListeningQuestion>(size)
                }
            }
    }
}