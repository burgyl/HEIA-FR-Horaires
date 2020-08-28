package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import ch.lburgy.heiafrschedule.type_converter.DateConverter;

@Entity
@TypeConverters(DateConverter.class)
public class ClassHEIAFR {
    @PrimaryKey
    @NonNull
    private final String name;
    private Date lastUpdate;

    public ClassHEIAFR(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
