package com.example.speak;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageQuestion implements Parcelable {
    private final String question;
    private final String correctAnswer;
    private final String[] options;
    private final String topic;
    private final String level;
    private final String imageResourceName;
    private String correctImageResource;

    public ImageQuestion(String question, String correctAnswer, String[] options, String topic, String level, String imageResourceName) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.options = options;
        this.topic = topic;
        this.level = level;
        this.imageResourceName = imageResourceName;
        this.correctImageResource = imageResourceName;
    }

    protected ImageQuestion(Parcel in) {
        question = in.readString();
        correctAnswer = in.readString();
        options = in.createStringArray();
        topic = in.readString();
        level = in.readString();
        imageResourceName = in.readString();
        correctImageResource = in.readString();
    }

    public static final Creator<ImageQuestion> CREATOR = new Creator<ImageQuestion>() {
        @Override
        public ImageQuestion createFromParcel(Parcel in) {
            return new ImageQuestion(in);
        }

        @Override
        public ImageQuestion[] newArray(int size) {
            return new ImageQuestion[size];
        }
    };

    public String getQuestion() { return question; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String[] getOptions() { return options; }
    public String getTopic() { return topic; }
    public String getLevel() { return level; }
    public String getImageResourceName() { return imageResourceName; }
    public String getCorrectImageResource() { return correctImageResource; }
    public void setCorrectImageResource(String correctImageResource) { this.correctImageResource = correctImageResource; }

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
        dest.writeString(imageResourceName);
        dest.writeString(correctImageResource);
    }
} 