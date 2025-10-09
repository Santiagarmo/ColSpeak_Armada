package com.example.speak;

import android.os.Parcel;
import android.os.Parcelable;

public class ListeningQuestion implements Parcelable {
    private final String question;
    private final String correctAnswer;
    private final String[] options;
    private final String topic;
    private final String level;
    private final boolean showText;
    private final String info;

    public ListeningQuestion(String question, String correctAnswer, String[] options, String topic, String level, boolean showText, String info) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.options = options;
        this.topic = topic;
        this.level = level;
        this.showText = showText;
        this.info = info;
    }

    protected ListeningQuestion(Parcel in) {
        question = in.readString();
        correctAnswer = in.readString();
        options = in.createStringArray();
        topic = in.readString();
        level = in.readString();
        showText = in.readByte() != 0;
        info = in.readString();
    }

    public static final Creator<ListeningQuestion> CREATOR = new Creator<ListeningQuestion>() {
        @Override
        public ListeningQuestion createFromParcel(Parcel in) {
            return new ListeningQuestion(in);
        }

        @Override
        public ListeningQuestion[] newArray(int size) {
            return new ListeningQuestion[size];
        }
    };

    public String getQuestion() {
        return question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String[] getOptions() {
        return options;
    }

    public String getTopic() { return topic; }
    public String getLevel() { return level; }
    public boolean isShowText() { return showText; }
    public String getInfo() { return info; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeString(correctAnswer);
        dest.writeStringArray(options);
        dest.writeString(topic);
        dest.writeString(level);
        dest.writeByte((byte) (showText ? 1 : 0));
        dest.writeString(info);
    }

} 