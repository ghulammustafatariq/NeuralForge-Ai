package com.superior.mindforgeai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String uid;
    private String topic;
    private String description;
    private String style;
    private int depth;
    private String aiResult;
    private long createdAt;
    @Nullable
    private String playlistId; // null = uncategorized

    public HistoryEntity() {}

    @Ignore
    public HistoryEntity(@NonNull String id, String uid, String topic, String description,
                         String style, int depth, String aiResult, long createdAt) {
        this.id = id;
        this.uid = uid;
        this.topic = topic;
        this.description = description;
        this.style = style;
        this.depth = depth;
        this.aiResult = aiResult;
        this.createdAt = createdAt;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    public String getAiResult() { return aiResult; }
    public void setAiResult(String aiResult) { this.aiResult = aiResult; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    @Nullable public String getPlaylistId() { return playlistId; }
    public void setPlaylistId(@Nullable String playlistId) { this.playlistId = playlistId; }
}
