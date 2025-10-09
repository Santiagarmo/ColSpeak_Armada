package com.example.speak;

public class QuizResult {
    private long userId;
    private String topic;
    private String level;
    private String quizType;
    private int score;
    private int totalQuestions;
    private double percentage;
    private long timestamp;

    public QuizResult() {
        // Constructor vacío requerido para Firebase
    }

    public QuizResult(long userId, String topic, String level, String quizType, 
                     int score, int totalQuestions, double percentage, long timestamp) {
        this.userId = userId;
        this.topic = topic;
        this.level = level;
        this.quizType = quizType;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
        this.timestamp = timestamp;
    }

    // Getters
    public long getUserId() { return userId; }
    public String getTopic() { return topic; }
    public String getLevel() { return level; }
    public String getQuizType() { return quizType; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public double getPercentage() { return percentage; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setUserId(long userId) { this.userId = userId; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setLevel(String level) { this.level = level; }
    public void setQuizType(String quizType) { this.quizType = quizType; }
    public void setScore(int score) { this.score = score; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 