package ch.lburgy.heiafrschedule.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.Lesson;
import ch.lburgy.heiafrschedule.database.LessonOfRoomComplete;
import ch.lburgy.heiafrschedule.database.RoomHEIAFR;

public class BottomSheetDialogLessonsOfRoom extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {

    private RoomHEIAFR room;
    private HashMap<Integer, ArrayList<LessonOfRoomComplete>> lessonsByDay;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_lessons_of_room, container, false);

        TextView txtRoom = v.findViewById(R.id.room);
        LinearLayout daysLayout = v.findViewById(R.id.days_layout);
        txtRoom.setText(room.getName());

        for (int day : lessonsByDay.keySet()) {
            if (lessonsByDay.get(day).size() == 0) continue;
            LinearLayout thisDayLayout = (LinearLayout) inflater.inflate(R.layout.sheet_lessons_of_room_day, container, false);
            TextView txtDay = thisDayLayout.findViewById(R.id.day);
            switch (day) {
                case 0:
                    txtDay.setText(getString(R.string.day_0));
                    break;
                case 1:
                    txtDay.setText(getString(R.string.day_1));
                    break;
                case 2:
                    txtDay.setText(getString(R.string.day_2));
                    break;
                case 3:
                    txtDay.setText(getString(R.string.day_3));
                    break;
                case 4:
                    txtDay.setText(getString(R.string.day_4));
                    break;
            }
            LinearLayout lessonsLayout = thisDayLayout.findViewById(R.id.lessons_layout);
            for (LessonOfRoomComplete lessonComplete : lessonsByDay.get(day)) {
                LinearLayout thisLessonLayout = (LinearLayout) inflater.inflate(R.layout.sheet_lessons_of_room_lesson, container, false);
                TextView txtName = thisLessonLayout.findViewById(R.id.name);
                TextView txtTime = thisLessonLayout.findViewById(R.id.time);
                TextView txtClasses = thisLessonLayout.findViewById(R.id.classes);

                Lesson lesson = lessonComplete.getLesson();
                txtName.setText(lesson.getName());
                txtTime.setText(String.format("%s - %s", lesson.getTimeStart(), lesson.getTimeEnd()));
                StringBuilder classes = new StringBuilder();
                for (int i = 0; i < lessonComplete.getClasses().size(); i++) {
                    if (i != 0)
                        classes.append(", ");
                    classes.append(lessonComplete.getClasses().get(i).getName());
                }
                txtClasses.setText(classes.toString());

                lessonsLayout.addView(thisLessonLayout);
            }
            daysLayout.addView(thisDayLayout);
        }

        getDialog().setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = d.findViewById(R.id.design_bottom_sheet);
                CoordinatorLayout coordinatorLayout = (CoordinatorLayout) bottomSheet.getParent();
                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight());
                coordinatorLayout.getParent().requestLayout();
            }
        });

        return v;
    }

    public BottomSheetDialogLessonsOfRoom() {
    }

    public void setRoom(RoomHEIAFR room) {
        this.room = room;
    }

    public void setLessonsByDay(HashMap<Integer, ArrayList<LessonOfRoomComplete>> lessonsByDay) {
        this.lessonsByDay = lessonsByDay;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
    }
}
