package com.superior.mindforgeai;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlaylistDao {

    @Query("SELECT * FROM playlists WHERE uid = :uid ORDER BY createdAt DESC")
    List<PlaylistEntity> getAllByUid(String uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlaylistEntity playlist);

    @Delete
    void delete(PlaylistEntity playlist);

    @Query("SELECT COUNT(*) FROM history WHERE uid = :uid AND playlistId = :playlistId")
    int countNotes(String uid, String playlistId);
}
