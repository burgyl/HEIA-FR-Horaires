package ch.lburgy.heiafrschedule.database;

import java.io.Serializable;
import java.util.List;

public class LessonOfRoomComplete implements Serializable {
    private final Lesson lesson;
    private final List<ClassHEIAFR> classes;
    private final List<Teacher> teachers;

    public LessonOfRoomComplete(Lesson lesson, List<ClassHEIAFR> classes, List<Teacher> teachers) {
        this.lesson = lesson;
        this.classes = classes;
        this.teachers = teachers;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public List<ClassHEIAFR> getClasses() {
        return classes;
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }
}
