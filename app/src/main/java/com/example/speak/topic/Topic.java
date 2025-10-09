package com.example.speak.topic;

import android.os.Parcel;
import android.os.Parcelable;

public class Topic implements Parcelable {
    private long id;
    private String name;
    private String level;
    private String description;

    public Topic(long id, String name, String level, String description) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.description = description;
    }

    protected Topic(Parcel in) {
        id = in.readLong();
        name = in.readString();
        level = in.readString();
        description = in.readString();
    }

    public static final Creator<Topic> CREATOR = new Creator<Topic>() {
        @Override
        public Topic createFromParcel(Parcel in) {
            return new Topic(in);
        }

        @Override
        public Topic[] newArray(int size) {
            return new Topic[size];
        }
    };

    // Getters
    public long getId() { return id; }
    public String getName() { return name; }
    public String getLevel() { return level; }
    public String getDescription() { return description; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(level);
        dest.writeString(description);
    }
} 