package ch.lburgy.heiafrschedule.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LessonTeacherJoinDao {

    @Insert
    void insertLessonTeacherJoin(LessonTeacherJoin LessonTeacherJoin);

    @Query("SELECT * FROM Teacher INNER JOIN LessonTeacherJoin ON Teacher.abbr = LessonTeacherJoin.teacherAbbr WHERE LessonTeacherJoin.lessonId =:lessonId")
    List<Teacher> getTeachersForLesson(final long lessonId);
}
