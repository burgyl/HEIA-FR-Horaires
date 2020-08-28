package ch.lburgy.heiafrschedule.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LessonDao {

    @Insert
    long insertLesson(Lesson lesson);

    @Query("DELETE FROM Lesson")
    void deleteLessons();

    @Query("SELECT * FROM Lesson")
    List<Lesson> getLessons();
}
