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

import java.util.List;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.LessonOfClassComplete;
import ch.lburgy.heiafrschedule.database.RoomHEIAFR;
import ch.lburgy.heiafrschedule.database.Teacher;

public class BottomSheetDialogLessonsInfos extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {

    private final LessonOfClassComplete lessonOfClassComplete;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_lessons_infos, container, false);

        TextView txtName = v.findViewById(R.id.name);
        TextView txtTime = v.findViewById(R.id.time);
        TextView txtRooms = v.findViewById(R.id.rooms);
        LinearLayout teachersLayout = v.findViewById(R.id.teachers_layout);

        txtName.setText(lessonOfClassComplete.getLesson().getName());
        txtTime.setText(String.format("%s - %s", lessonOfClassComplete.getLesson().getTimeStart(), lessonOfClassComplete.getLesson().getTimeEnd()));
        StringBuilder rooms = new StringBuilder();
        List<RoomHEIAFR> lessonCompleteRooms = lessonOfClassComplete.getRooms();
        for (int i = 0; i < lessonCompleteRooms.size(); i++) {
            if (i != 0)
                rooms.append(", ");
            rooms.append(lessonCompleteRooms.get(i).getId());
        }
        txtRooms.setText(rooms.toString());

        for (Teacher teacher : lessonOfClassComplete.getTeachers()) {
            LinearLayout teacherLayout = (LinearLayout) inflater.inflate(R.layout.sheet_lessons_infos_teachers, null);

            TextView txtNameTeacher = teacherLayout.findViewById(R.id.name);
            TextView txtEmailTeacher = teacherLayout.findViewById(R.id.email);
            TextView txtPhoneTeacher = teacherLayout.findViewById(R.id.phone);
            TextView txtOfficeTeacher = teacherLayout.findViewById(R.id.office);

            txtNameTeacher.setText(teacher.getName());
            txtEmailTeacher.setText(teacher.getEmail());
            String phone = teacher.getPhone();
            if (phone != null && !phone.equals("")) {
                txtPhoneTeacher.setText(phone);
                txtPhoneTeacher.setVisibility(View.VISIBLE);
            }
            String office = teacher.getOffice();
            if (office != null && !office.equals("")) {
                txtOfficeTeacher.setText(office);
                txtOfficeTeacher.setVisibility(View.VISIBLE);
            }

            teachersLayout.addView(teacherLayout);
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

    public BottomSheetDialogLessonsInfos(LessonOfClassComplete lessonOfClassComplete) {
        this.lessonOfClassComplete = lessonOfClassComplete;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
    }
}
