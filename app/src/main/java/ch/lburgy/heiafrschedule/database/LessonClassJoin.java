package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import org.jetbrains.annotations.NotNull;

@Entity(primaryKeys = {"lessonID", "className"},
        indices = {@Index("className")},
        foreignKeys = {
                @ForeignKey(entity = Lesson.class,
                        parentColumns = "id",
                        childColumns = "lessonID",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = ClassHEIAFR.class,
                        parentColumns = "name",
                        childColumns = "className",
                        onDelete = ForeignKey.CASCADE)
        })
public class LessonClassJoin {

    private final long lessonID;
    @NonNull
    private final String className;

    public LessonClassJoin(final long lessonID, @NotNull final String className) {
        this.lessonID = lessonID;
        this.className = className;
    }

    public long getLessonID() {
        return lessonID;
    }

    @NonNull
    public String getClassName() {
        return className;
    }
}