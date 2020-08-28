package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity()
public class Lesson implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String timeStart;
    private String timeEnd;
    private int dayOfTheWeek;

    @Ignore
    public Lesson() {
    }

    public Lesson(String name, String timeStart, String timeEnd, int dayOfTheWeek) {
        this.name = name;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.dayOfTheWeek = dayOfTheWeek;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public int getDayOfTheWeek() {
        return dayOfTheWeek;
    }

    public void setDayOfTheWeek(int dayOfTheWeek) {
        this.dayOfTheWeek = dayOfTheWeek;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (getClass() != obj.getClass()) return false;
        Lesson lesson = (Lesson) obj;
        return name.equals(lesson.name) && timeStart.equals(lesson.timeStart) && timeEnd.equals(lesson.timeEnd) && dayOfTheWeek == lesson.dayOfTheWeek;
    }

    @NonNull
    @Override
    public String toString() {
        return dayOfTheWeek + " - " + timeStart + " - " + timeEnd + " - " + name;
    }
}
