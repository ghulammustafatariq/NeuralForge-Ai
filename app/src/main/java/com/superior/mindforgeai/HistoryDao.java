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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity entity);

    @Delete
    void delete(HistoryEntity entity);

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    HistoryEntity getById(String id);
}
