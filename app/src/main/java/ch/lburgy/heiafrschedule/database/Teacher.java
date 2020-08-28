package ch.lburgy.heiafrschedule.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Entity
public class Teacher implements Serializable {
    @PrimaryKey
    @NonNull
    private final String abbr;
    @NonNull
    private final String name;
    @NonNull
    private final String email;
    private final String phone;
    @ForeignKey(entity = RoomHEIAFR.class,
            parentColumns = "name",
            childColumns = "office",
            onDelete = ForeignKey.CASCADE)
    private final String office;

    public Teacher(@NonNull String abbr, @NonNull String name, @NonNull String email, String phone, String office) {
        this.abbr = abbr;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.office = office;
    }

    @NonNull
    public String getAbbr() {
        return abbr;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getOffice() {
        return office;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (getClass() != obj.getClass()) return false;
        Teacher teacher = (Teacher) obj;
        return abbr.equals(teacher.abbr);
    }

    @NonNull
    @Override
    public String toString() {
        String out = abbr + ": " + name + " - " + email;
        if (phone != null)
            out += " - " + phone;
        if (phone != null)
            out += " - " + office;
        return out;
    }
}
