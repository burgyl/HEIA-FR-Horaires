package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import org.jetbrains.annotations.NotNull;

@Entity(primaryKeys = {"lessonId", "teacherAbbr"},
        indices = {@Index("teacherAbbr")},
        foreignKeys = {
                @ForeignKey(entity = Lesson.class,
                        parentColumns = "id",
                        childColumns = "lessonId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Teacher.class,
                        parentColumns = "abbr",
                        childColumns = "teacherAbbr",
                        onDelete = ForeignKey.CASCADE)
        })
public class LessonTeacherJoin {

    private final long lessonId;
    @NonNull
    private final String teacherAbbr;

    public LessonTeacherJoin(final long lessonId, @NotNull final String teacherAbbr) {
        this.lessonId = lessonId;
        this.teacherAbbr = teacherAbbr;
    }

    public long getLessonId() {
        return lessonId;
    }

    @NonNull
    public String getTeacherAbbr() {
        return teacherAbbr;
    }
}