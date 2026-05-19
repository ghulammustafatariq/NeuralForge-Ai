package com.superior.mindforgeai;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {HistoryEntity.class, PlaylistEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract HistoryDao historyDao();
    public abstract PlaylistDao playlistDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add playlistId column to existing history table
            database.execSQL("ALTER TABLE history ADD COLUMN playlistId TEXT");
            // Create new playlists table
            database.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (" +
                    "`id` TEXT NOT NULL, " +
                    "`uid` TEXT, " +
                    "`name` TEXT, " +
                    "`color` TEXT, " +
                    "`emoji` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "mindforge_db")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
