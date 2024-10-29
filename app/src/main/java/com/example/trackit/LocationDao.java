package com.example.trackit;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    void insert(LocationEntity locationEntity);

    @Query("SELECT * FROM locations")
    List<LocationEntity> getAllLocations();
}
