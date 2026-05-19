package com.superior.mindforgeai;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String uid;
    private String name;
    private String color;   // hex e.g. #6366F1
    private String emoji;   // single emoji character
    private long createdAt;

    public PlaylistEntity() {}

    @Ignore
    public PlaylistEntity(@NonNull String id, String uid, String name,
                          String color, String emoji, long createdAt) {
        this.id = id;
        this.uid = uid;
        this.name = name;
        this.color = color;
        this.emoji = emoji;
        this.createdAt = createdAt;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
