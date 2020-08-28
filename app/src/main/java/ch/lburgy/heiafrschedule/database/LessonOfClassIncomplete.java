package ch.lburgy.heiafrschedule.database;

import java.util.List;

public class LessonOfClassIncomplete {
    private final Lesson lesson;
    private final List<String> roomsIDs;
    private final List<String> teachersAbbrs;

    public LessonOfClassIncomplete(Lesson lesson, List<String> roomsIDs, List<String> teachersAbbrs) {
        this.lesson = lesson;
        this.roomsIDs = roomsIDs;
        this.teachersAbbrs = teachersAbbrs;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public List<String> getRoomsIDs() {
        return roomsIDs;
    }

    public List<String> getTeachersAbbrs() {
        return teachersAbbrs;
    }
}
