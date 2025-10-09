package com.example.speak.pronunciation;

public class PronunciationQuestion {
    private String question;
    private String topic;
    private String level;

    public PronunciationQuestion(String question, String topic, String level) {
        this.question = question;
        this.topic = topic;
        this.level = level;
    }

    public String getQuestion() {
        return question;
    }

    public String getTopic() {
        return topic;
    }

    public String getLevel() {
        return level;
    }
} 