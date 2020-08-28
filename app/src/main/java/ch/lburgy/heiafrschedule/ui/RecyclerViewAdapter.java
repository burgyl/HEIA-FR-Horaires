package ch.lburgy.heiafrschedule.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.LessonOfRoomComplete;
import ch.lburgy.heiafrschedule.database.RoomHEIAFR;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private final ArrayList<RoomHEIAFR> rooms;
    private final HashMap<RoomHEIAFR, HashMap<Integer, ArrayList<LessonOfRoomComplete>>> lessonsByRoom;
    private final FragmentManager fragmentManager;
    private final BottomSheetDialogLessonsOfRoom bottomSheetDialog;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView txtRoomName;

        public ViewHolder(View v) {
            super(v);
            txtRoomName = v.findViewById(R.id.room_name);
        }

        public void bind(final ArrayList<RoomHEIAFR> rooms, final HashMap<RoomHEIAFR, HashMap<Integer, ArrayList<LessonOfRoomComplete>>> lessonsByRoom,
                         final BottomSheetDialogLessonsOfRoom bottomSheetDialog, final FragmentManager fragmentManager, final int position) {
            RoomHEIAFR room = rooms.get(position);
            txtRoomName.setText(room.getName());
            txtRoomName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RoomHEIAFR room = rooms.get(position);
                    bottomSheetDialog.setRoom(room);
                    bottomSheetDialog.setLessonsByDay(lessonsByRoom.get(room));
                    bottomSheetDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
                    bottomSheetDialog.show(fragmentManager, bottomSheetDialog.getTag());
                }
            });
        }
    }

    public RecyclerViewAdapter(ArrayList<RoomHEIAFR> rooms, HashMap<RoomHEIAFR, HashMap<Integer, ArrayList<LessonOfRoomComplete>>> lessonsByRoom,
                               BottomSheetDialogLessonsOfRoom bottomSheetDialog, FragmentManager fragmentManager) {
        this.rooms = rooms;
        this.lessonsByRoom = lessonsByRoom;
        this.fragmentManager = fragmentManager;
        this.bottomSheetDialog = bottomSheetDialog;
    }

    @NotNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cell_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(rooms, lessonsByRoom, bottomSheetDialog, fragmentManager, position);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }
}
