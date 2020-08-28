package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LessonRoomJoinDao {

    @Insert
    void insertLessonRoomJoin(LessonRoomJoin lessonRoomJoin);

    @Query("SELECT * FROM RoomHEIAFR INNER JOIN LessonRoomJoin ON RoomHEIAFR.id = LessonRoomJoin.roomID WHERE LessonRoomJoin.lessonID =:lessonID")
    List<RoomHEIAFR> getRoomsForLesson(final long lessonID);

    @Query("SELECT * FROM Lesson INNER JOIN LessonRoomJoin ON Lesson.id = LessonRoomJoin.lessonID WHERE LessonRoomJoin.roomID =:roomID")
    List<Lesson> getLessonsForRoom(final @NonNull String roomID);
}
