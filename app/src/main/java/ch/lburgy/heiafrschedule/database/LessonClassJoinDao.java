package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LessonClassJoinDao {

    @Insert
    void insertLessonClassJoin(LessonClassJoin lessonClassJoin);

    @Query("DELETE FROM LessonClassJoin WHERE className=:className")
    void deleteLessonsFromClass(final @NonNull String className);

    @Query("DELETE FROM Lesson WHERE id NOT IN (" +
            "SELECT id FROM Lesson INNER JOIN LessonClassJoin ON Lesson.id = LessonClassJoin.lessonID)")
    void deleteLessonsNoClass();

    @Query("DELETE FROM Lesson WHERE id NOT IN (" +
            "SELECT id FROM Lesson INNER JOIN LessonClassJoin ON Lesson.id = LessonClassJoin.lessonID INNER JOIN ClassHEIAFR ON ClassHEIAFR.name = LessonClassJoin.className" +
            " WHERE ClassHEIAFR.lastUpdate != -1)")
    void deleteLessonsNoClassUpdate();

    @Query("SELECT * FROM ClassHEIAFR INNER JOIN LessonClassJoin ON ClassHEIAFR.name = LessonClassJoin.className WHERE LessonClassJoin.lessonID =:lessonID")
    List<ClassHEIAFR> getClassesForLesson(final long lessonID);

    @Query("SELECT * FROM Lesson INNER JOIN LessonClassJoin ON Lesson.id = LessonClassJoin.lessonID WHERE LessonClassJoin.className =:className")
    List<Lesson> getLessonsForClass(final @NonNull String className);
}
