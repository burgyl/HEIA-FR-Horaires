package ch.lburgy.heiafrschedule.database;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

public class LessonOfClassComplete implements Serializable {
    private final Lesson lesson;
    private final List<RoomHEIAFR> rooms;
    private final List<Teacher> teachers;

    public LessonOfClassComplete(Lesson lesson, List<RoomHEIAFR> rooms, List<Teacher> teachers) {
        this.lesson = lesson;
        this.rooms = rooms;
        this.teachers = teachers;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public List<RoomHEIAFR> getRooms() {
        return rooms;
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public boolean equals(@Nullable LessonOfClassComplete lessonOfClassComplete) {
        return lesson.equals(lessonOfClassComplete.lesson) && rooms.equals(lessonOfClassComplete.rooms) && teachers.equals(lessonOfClassComplete.teachers);
    }
}
