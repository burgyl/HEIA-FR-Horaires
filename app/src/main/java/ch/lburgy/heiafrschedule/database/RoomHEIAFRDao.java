package ch.lburgy.heiafrschedule.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RoomHEIAFRDao {

    @Insert
    void insertRoom(RoomHEIAFR room);

    @Query("SELECT * FROM RoomHEIAFR WHERE id=:id")
    RoomHEIAFR getRoom(String id);

    @Query("SELECT count(*) FROM RoomHEIAFR")
    int getNbRoom();

    @Query("SELECT * FROM RoomHEIAFR")
    List<RoomHEIAFR> getRooms();

    @Insert
    void insertRooms(List<RoomHEIAFR> roomsHEIAFR);

    @Query("DELETE FROM RoomHEIAFR")
    void deleteRooms();
}
