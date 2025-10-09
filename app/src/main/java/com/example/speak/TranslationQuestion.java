package com.example.speak;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class TranslationQuestion implements Parcelable {
    private String englishText;
    private List<String> spanishWords;
    private List<String> correctTranslation;
    private String topic;
    private String level;

    // Constructor principal
    public TranslationQuestion(String englishText, List<String> spanishWords, List<String> correctTranslation, String topic, String level) {
        this.englishText = englishText;
        this.spanishWords = spanishWords != null ? spanishWords : new ArrayList<>();
        this.correctTranslation = correctTranslation != null ? correctTranslation : new ArrayList<>();
        this.topic = topic;
        this.level = level;
    }

    // Constructor simplificado
    public TranslationQuestion(String englishText, String topic, String level) {
        this.englishText = englishText;
        this.spanishWords = new ArrayList<>();
        this.correctTranslation = new ArrayList<>();
        this.topic = topic;
        this.level = level;
    }

    // Getters
    public String getEnglishText() {
        return englishText;
    }

    public List<String> getSpanishWords() {
        return spanishWords;
    }

    public List<String> getCorrectTranslation() {
        return correctTranslation;
    }

    public String getTopic() {
        return topic;
    }

    public String getLevel() {
        return level;
    }

    // Setters
    public void setEnglishText(String englishText) {
        this.englishText = englishText;
    }

    public void setSpanishWords(List<String> spanishWords) {
        this.spanishWords = spanishWords != null ? spanishWords : new ArrayList<>();
    }

    public void setCorrectTranslation(List<String> correctTranslation) {
        this.correctTranslation = correctTranslation != null ? correctTranslation : new ArrayList<>();
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    // Métodos útiles
    public void addSpanishWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            this.spanishWords.add(word.trim());
        }
    }

    public void addCorrectTranslationWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            this.correctTranslation.add(word.trim());
        }
    }

    public String getCorrectTranslationAsString() {
        return String.join(" ", correctTranslation);
    }

    // Implementación de Parcelable
    protected TranslationQuestion(Parcel in) {
        englishText = in.readString();
        spanishWords = in.createStringArrayList();
        correctTranslation = in.createStringArrayList();
        topic = in.readString();
        level = in.readString();
    }

    public static final Creator<TranslationQuestion> CREATOR = new Creator<TranslationQuestion>() {
        @Override
        public TranslationQuestion createFromParcel(Parcel in) {
            return new TranslationQuestion(in);
        }

        @Override
        public TranslationQuestion[] newArray(int size) {
            return new TranslationQuestion[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(englishText);
        dest.writeStringList(spanishWords);
        dest.writeStringList(correctTranslation);
        dest.writeString(topic);
        dest.writeString(level);
    }

    @Override
    public String toString() {
        return "TranslationQuestion{" +
                "englishText='" + englishText + '\'' +
                ", spanishWords=" + spanishWords +
                ", correctTranslation=" + correctTranslation +
                ", topic='" + topic + '\'' +
                ", level='" + level + '\'' +
                '}';
    }
} 