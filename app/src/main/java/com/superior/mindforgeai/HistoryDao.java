package com.superior.mindforgeai;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {

    @Query("SELECT * FROM history WHERE uid = :uid ORDER BY createdAt DESC")
    List<HistoryEntity> getAllByUid(String uid);

    @Query("SELECT * FROM history WHERE uid = :uid AND playlistId = :playlistId ORDER BY createdAt DESC")
    List<HistoryEntity> getByPlaylistId(String uid, String playlistId);

    @Query("SELECT * FROM history WHERE uid = :uid AND (playlistId IS NULL OR playlistId = '') ORDER BY createdAt DESC")
    List<HistoryEntity> getUncategorized(String uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity entity);

    @Delete
    void delete(HistoryEntity entity);

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    HistoryEntity getById(String id);

    @Query("UPDATE history SET playlistId = :playlistId WHERE id = :noteId")
    void setPlaylist(String noteId, String playlistId);

    @Query("SELECT COUNT(*) FROM history WHERE uid = :uid AND playlistId = :playlistId")
    int countByPlaylist(String uid, String playlistId);
}
