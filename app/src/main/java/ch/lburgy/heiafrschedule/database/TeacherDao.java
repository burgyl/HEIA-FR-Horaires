package ch.lburgy.heiafrschedule.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface TeacherDao {

    @Insert
    void insertTeacher(Teacher teacher);

    @Query("SELECT * FROM Teacher WHERE abbr=:abbr")
    Teacher getTeacher(String abbr);

    @Query("DELETE FROM Teacher")
    void deleteTeachers();
}
