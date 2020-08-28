package ch.lburgy.heiafrschedule.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ClassHEIAFRDao {

    @Insert
    void insertClass(ClassHEIAFR classHEIAFR);

    @Insert
    void insertClasses(List<ClassHEIAFR> classesHEIAFR);

    @Update
    void updateClassHEIAFR(ClassHEIAFR classHEIAFR);

    @Query("DELETE FROM ClassHEIAFR")
    void deleteClasses();

    @Query("SELECT * FROM ClassHEIAFR")
    List<ClassHEIAFR> getClasses();

    @Query("SELECT * FROM ClassHEIAFR WHERE ClassHEIAFR.name=:className")
    ClassHEIAFR getClass(String className);
}
