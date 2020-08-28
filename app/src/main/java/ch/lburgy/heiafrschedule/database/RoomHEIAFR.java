package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Comparator;

@Entity
public class RoomHEIAFR implements Serializable {
    @PrimaryKey
    @NonNull
    private final String id;
    private String name;

    @Ignore
    public RoomHEIAFR(@NotNull String id) {
        this.id = id;
        name = null;
    }

    public RoomHEIAFR(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (getClass() != obj.getClass()) return false;
        RoomHEIAFR room = (RoomHEIAFR) obj;
        return id.equals(room.id);
    }

    public static final Comparator<RoomHEIAFR> ROOM_HEIAFR_COMPARATOR = new Comparator<RoomHEIAFR>() {
        @Override
        public int compare(RoomHEIAFR o1, RoomHEIAFR o2) {
            return o1.id.compareTo(o2.id);
        }
    };
}
