package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import org.jetbrains.annotations.NotNull;

@Entity(primaryKeys = {"lessonID", "roomID"},
        indices = {@Index("roomID")},
        foreignKeys = {
                @ForeignKey(entity = Lesson.class,
                        parentColumns = "id",
                        childColumns = "lessonID",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = RoomHEIAFR.class,
                        parentColumns = "id",
                        childColumns = "roomID",
                        onDelete = ForeignKey.CASCADE)
        })
public class LessonRoomJoin {

    private final long lessonID;
    @NonNull
    private final String roomID;

    public LessonRoomJoin(final long lessonID, @NotNull final String roomID) {
        this.lessonID = lessonID;
        this.roomID = roomID;
    }

    public long getLessonID() {
        return lessonID;
    }

    @NonNull
    public String getRoomID() {
        return roomID;
    }
}