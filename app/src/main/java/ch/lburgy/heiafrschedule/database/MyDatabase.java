package ch.lburgy.heiafrschedule.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities =
        {ClassHEIAFR.class, Lesson.class, LessonClassJoin.class, RoomHEIAFR.class, LessonRoomJoin.class, Teacher.class, LessonTeacherJoin.class},
        version = 1, exportSchema = false)
public abstract class MyDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "HEIAFRScheduleDatabase.db";
    private static volatile MyDatabase instance;

    public static synchronized MyDatabase getInstance(Context context) {
        if (instance == null)
            instance = create(context);
        return instance;
    }

    private static MyDatabase create(final Context context) {
        return Room.databaseBuilder(context, MyDatabase.class, DATABASE_NAME).build();
    }

    public abstract ClassHEIAFRDao getClassHEIAFRDao();

    public abstract LessonDao getLessonDao();

    public abstract RoomHEIAFRDao getRoomHEIAFRDao();

    public abstract LessonRoomJoinDao getLessonRoomJoinDao();

    public abstract TeacherDao getTeacherDao();

    public abstract LessonTeacherJoinDao getLessonTeacherJoinDao();

    public abstract LessonClassJoinDao getLessonClassJoinDao();
}
