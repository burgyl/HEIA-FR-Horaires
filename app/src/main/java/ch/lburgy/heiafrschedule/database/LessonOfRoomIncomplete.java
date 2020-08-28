package ch.lburgy.heiafrschedule.database;

import java.util.List;

public class LessonOfRoomIncomplete {
    private final Lesson lesson;
    private final List<String> classIDs;
    private final List<String> teachersAbbrs;

    public LessonOfRoomIncomplete(Lesson lesson, List<String> classIDs, List<String> teachersAbbrs) {
        this.lesson = lesson;
        this.classIDs = classIDs;
        this.teachersAbbrs = teachersAbbrs;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public List<String> getClassIDs() {
        return classIDs;
    }

    public List<String> getTeachersAbbrs() {
        return teachersAbbrs;
    }
}
