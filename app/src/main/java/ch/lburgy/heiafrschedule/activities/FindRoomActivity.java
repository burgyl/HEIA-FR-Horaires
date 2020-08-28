package ch.lburgy.heiafrschedule.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.ClassHEIAFR;
import ch.lburgy.heiafrschedule.database.ClassHEIAFRDao;
import ch.lburgy.heiafrschedule.database.Lesson;
import ch.lburgy.heiafrschedule.database.LessonClassJoin;
import ch.lburgy.heiafrschedule.database.LessonClassJoinDao;
import ch.lburgy.heiafrschedule.database.LessonDao;
import ch.lburgy.heiafrschedule.database.LessonOfRoomComplete;
import ch.lburgy.heiafrschedule.database.LessonOfRoomIncomplete;
import ch.lburgy.heiafrschedule.database.LessonRoomJoin;
import ch.lburgy.heiafrschedule.database.LessonRoomJoinDao;
import ch.lburgy.heiafrschedule.database.LessonTeacherJoin;
import ch.lburgy.heiafrschedule.database.LessonTeacherJoinDao;
import ch.lburgy.heiafrschedule.database.MyDatabase;
import ch.lburgy.heiafrschedule.database.RoomHEIAFR;
import ch.lburgy.heiafrschedule.database.RoomHEIAFRDao;
import ch.lburgy.heiafrschedule.database.Teacher;
import ch.lburgy.heiafrschedule.database.TeacherDao;
import ch.lburgy.heiafrschedule.http.HttpBasicClient;
import ch.lburgy.heiafrschedule.preferences.PrefManager;
import ch.lburgy.heiafrschedule.ui.BottomSheetDialogLessonsOfRoom;
import ch.lburgy.heiafrschedule.ui.RecyclerViewAdapter;

public class FindRoomActivity extends AppCompatActivity {

    private static final int CODE_NO_PROBLEMS = -1;
    private static final int CODE_NO_CONNECTION = -2;

    private static final int NB_DAYS = 5;

    private static final String KEY_SAVED_ROOMS = "rooms";
    private static final String KEY_SAVED_LESSONS_ROOMS = "lessons_rooms";
    private static final String KEY_SAVED_DAY_SELECTED = "day_selected";
    private static final String KEY_SAVED_HOUR_START = "hour_start";
    private static final String KEY_SAVED_MINUTES_START = "minutes_start";
    private static final String KEY_SAVED_HOUR_END = "hour_end";
    private static final String KEY_SAVED_MINUTES_END = "minutes_end";
    private static final String KEY_SAVED_THREAD_ALIVE = "thread_alive";
    private static final String KEY_SAVED_THREAD_TYPE = "thread_type";

    private HttpBasicClient httpBasicClient;
    private PrefManager prefManager;
    private ArrayList<RoomHEIAFR> freeRooms;
    private HashMap<RoomHEIAFR, HashMap<Integer, ArrayList<LessonOfRoomComplete>>> lessonsByRoom;
    private RecyclerViewAdapter adapter;
    private ProgressDialog progressDialog;
    private BottomSheetDialogLessonsOfRoom bottomSheetDialog;

    private LessonDao lessonDao;
    private RoomHEIAFRDao roomHEIAFRDao;
    private LessonRoomJoinDao lessonRoomJoinDao;
    private TeacherDao teacherDao;
    private LessonTeacherJoinDao lessonTeacherJoinDao;
    private ClassHEIAFRDao classHEIAFRDao;
    private LessonClassJoinDao lessonClassJoinDao;

    private Spinner spinnerDay;
    private TextView txtStartHour;
    private TextView txtStartMinutes;
    private TextView txtEndHour;
    private TextView txtEndMinutes;
    private int startHour;
    private int startMinutes;
    private int endHour;
    private int endMinutes;
    private Thread currentThread;
    private ThreadType threadType;

    public enum ThreadType {
        refreshLessons,
        searchRooms;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_room);

        prefManager = new PrefManager(this);
        httpBasicClient = new HttpBasicClient(this, prefManager.getUsername(), prefManager.getPassword());
        MyDatabase myDatabase = MyDatabase.getInstance(this);
        lessonDao = myDatabase.getLessonDao();
        roomHEIAFRDao = myDatabase.getRoomHEIAFRDao();
        lessonRoomJoinDao = myDatabase.getLessonRoomJoinDao();
        teacherDao = myDatabase.getTeacherDao();
        lessonTeacherJoinDao = myDatabase.getLessonTeacherJoinDao();
        classHEIAFRDao = myDatabase.getClassHEIAFRDao();
        lessonClassJoinDao = myDatabase.getLessonClassJoinDao();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        spinnerDay = findViewById(R.id.day);

        String[] days = {getResources().getString(R.string.day_0), getResources().getString(R.string.day_1),
                getResources().getString(R.string.day_2), getResources().getString(R.string.day_3),
                getResources().getString(R.string.day_4)};
        ArrayAdapter aa = new ArrayAdapter(this, R.layout.spinner_item, days);
        aa.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDay.setAdapter(aa);
        if (savedInstanceState != null) {
            spinnerDay.setSelection(savedInstanceState.getInt(KEY_SAVED_DAY_SELECTED));
        } else {
            switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                case Calendar.TUESDAY:
                    spinnerDay.setSelection(1);
                    break;
                case Calendar.WEDNESDAY:
                    spinnerDay.setSelection(2);
                    break;
                case Calendar.THURSDAY:
                    spinnerDay.setSelection(3);
                    break;
                case Calendar.FRIDAY:
                    spinnerDay.setSelection(4);
                    break;
                default:
                    spinnerDay.setSelection(0);
                    break;
            }
        }

        LinearLayout layoutTimeStart = findViewById(R.id.time_start);
        LinearLayout layoutTimeEnd = findViewById(R.id.time_end);

        txtStartHour = layoutTimeStart.findViewById(R.id.hour);
        txtStartMinutes = layoutTimeStart.findViewById(R.id.minutes);
        txtEndHour = layoutTimeEnd.findViewById(R.id.hour);
        txtEndMinutes = layoutTimeEnd.findViewById(R.id.minutes);

        if (savedInstanceState != null) {
            startHour = savedInstanceState.getInt(KEY_SAVED_HOUR_START);
            startMinutes = savedInstanceState.getInt(KEY_SAVED_HOUR_END);
            endHour = savedInstanceState.getInt(KEY_SAVED_HOUR_END);
            endMinutes = savedInstanceState.getInt(KEY_SAVED_MINUTES_END);
            txtStartHour.setText(String.format("%02d", startHour));
            txtStartMinutes.setText(String.format("%02d", startMinutes));
            txtEndHour.setText(String.format("%02d", endHour));
            txtEndMinutes.setText(String.format("%02d", endMinutes));
        } else {
            Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minutes = currentTime.get(Calendar.MINUTE);

            startHour = hour;
            startMinutes = minutes;
            txtStartHour.setText(String.format("%02d", startHour));
            txtStartMinutes.setText(String.format("%02d", startMinutes));

            hour++;
            if (hour > 23) {
                hour = 23;
                minutes = 59;
            }

            txtEndHour.setText(String.format("%02d", hour));
            txtEndMinutes.setText(String.format("%02d", minutes));
            endHour = hour;
            endMinutes = minutes;
        }

        layoutTimeStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(FindRoomActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        txtStartHour.setText(String.format("%02d", selectedHour));
                        txtStartMinutes.setText(String.format("%02d", selectedMinute));
                        int diffHours = selectedHour - startHour;
                        int diffMinutes = selectedMinute - startMinutes;
                        startHour = selectedHour;
                        startMinutes = selectedMinute;
                        endHour += diffHours;
                        endMinutes += diffMinutes;
                        if (endMinutes > 59) {
                            endHour++;
                            endMinutes -= 60;
                        } else if (endMinutes < 0) {
                            endHour--;
                            endMinutes += 60;
                        }
                        if (endHour > 23) {
                            endHour = 23;
                            endMinutes = 59;
                        } else if (endHour < 0) {
                            endHour = 0;
                            endMinutes = 0;
                        }
                        txtEndHour.setText(String.format("%02d", endHour));
                        txtEndMinutes.setText(String.format("%02d", endMinutes));
                    }
                }, startHour, startMinutes, true).show();
            }
        });
        layoutTimeEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(FindRoomActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        if (startHour < selectedHour || (startHour == selectedHour && startMinutes <= selectedMinute)) {
                            txtEndHour.setText(String.format("%02d", selectedHour));
                            txtEndMinutes.setText(String.format("%02d", selectedMinute));
                            endHour = selectedHour;
                            endMinutes = selectedMinute;
                        } else {
                            Toast.makeText(FindRoomActivity.this, getResources().getString(R.string.error_end_time_before_start), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, endHour, endMinutes, true).show();
            }
        });

        ImageButton btnSearch = findViewById(R.id.search_button);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRooms();
            }
        });

        if (savedInstanceState != null) {
            freeRooms = (ArrayList<RoomHEIAFR>) savedInstanceState.getSerializable(KEY_SAVED_ROOMS);
            lessonsByRoom = (HashMap<RoomHEIAFR, HashMap<Integer, ArrayList<LessonOfRoomComplete>>>) savedInstanceState.getSerializable(KEY_SAVED_LESSONS_ROOMS);

            if (savedInstanceState.getBoolean(KEY_SAVED_THREAD_ALIVE, false)) {
                threadType = (ThreadType) savedInstanceState.getSerializable(KEY_SAVED_THREAD_TYPE);
                switch (threadType) {
                    case searchRooms:
                        searchRooms();
                        break;
                    case refreshLessons:
                        refreshLessons();
                        break;
                }
            }
        } else {
            freeRooms = new ArrayList<>();
            lessonsByRoom = new HashMap<>();
        }
        RecyclerView recyclerView = findViewById(R.id.rooms);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        bottomSheetDialog = new BottomSheetDialogLessonsOfRoom();
        adapter = new RecyclerViewAdapter(freeRooms, lessonsByRoom, bottomSheetDialog, getSupportFragmentManager());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_find_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh_lessons:
                refreshLessons();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshLessons() {
        String progressTitle = getResources().getString(R.string.progress_refresh_lessons_title);
        progressDialog = ProgressDialog.show(this, progressTitle, null, true);
        threadType = ThreadType.refreshLessons;
        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int httpCode = CODE_NO_PROBLEMS;
                if (httpBasicClient.isConnectedToInternet()) {
                    try {
                        lessonClassJoinDao.deleteLessonsNoClassUpdate();
                        getRoomsFromInternet(new ArrayList<>(roomHEIAFRDao.getRooms()));
                    } catch (HttpBasicClient.HttpException e) {
                        httpCode = e.getCode();
                    } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                        httpCode = CODE_NO_CONNECTION;
                    } catch (InterruptedIOException e) {
                        // do nothing
                    }
                } else {
                    httpCode = CODE_NO_CONNECTION;
                }

                final int finalHttpCode = httpCode;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!FindRoomActivity.this.isDestroyed() && progressDialog != null && progressDialog.isShowing())
                            progressDialog.dismiss();
                        if (finalHttpCode != CODE_NO_PROBLEMS)
                            showHttpErrorCode(finalHttpCode);
                    }
                });
            }
        });
        currentThread.start();
    }

    private void searchRooms() {
        String progressTitle = getResources().getString(R.string.progress_search_rooms_title);
        progressDialog = ProgressDialog.show(this, progressTitle, null, true);
        searchRooms(spinnerDay.getSelectedItemPosition(), startHour, startMinutes, endHour, endMinutes);
    }

    private void searchRooms(final int day, final int hourStart, final int minutesStart, final int hourEnd, final int minutesEnd) {
        threadType = ThreadType.searchRooms;
        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int httpCode = CODE_NO_PROBLEMS;

                    Date lastUpdate = prefManager.getLastUpdateRoomsLessons();
                    ArrayList<RoomHEIAFR> rooms = new ArrayList<>(roomHEIAFRDao.getRooms());
                    if (rooms.size() == 0) {
                        try {
                            rooms = new ArrayList<>(httpBasicClient.getRooms());
                            roomHEIAFRDao.insertRooms(rooms);
                        } catch (HttpBasicClient.HttpException e) {
                            httpCode = e.getCode();
                        } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                            httpCode = CODE_NO_CONNECTION;
                        }
                    }
                    if (lessonsByRoom.size() == 0) {
                        if (lastUpdate == null) {
                            try {
                                getRoomsFromInternet(rooms);
                            } catch (HttpBasicClient.HttpException e) {
                                httpCode = e.getCode();
                            } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                                httpCode = CODE_NO_CONNECTION;
                            }
                        } else {
                            getRoomsFromDB(rooms);
                        }
                    }

                    updateRoomFreeList(day, hourStart, minutesStart, hourEnd, minutesEnd);

                    final int finalHttpCode = httpCode;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!FindRoomActivity.this.isDestroyed() && progressDialog != null && progressDialog.isShowing())
                                progressDialog.dismiss();
                            if (finalHttpCode == CODE_NO_PROBLEMS) {
                                adapter.notifyDataSetChanged();
                            } else {
                                showHttpErrorCode(finalHttpCode);
                            }
                        }
                    });
                } catch (InterruptedIOException e) {
                    // do nothing
                }
            }
        });
        currentThread.start();
    }

    private void getRoomsFromDB(ArrayList<RoomHEIAFR> rooms) {
        for (RoomHEIAFR room : rooms) {
            HashMap<Integer, ArrayList<LessonOfRoomComplete>> lessonsCompletes = new HashMap<>();
            for (int i = 0; i < NB_DAYS; i++)
                lessonsCompletes.put(i, new ArrayList<LessonOfRoomComplete>());

            ArrayList<Lesson> lessons = new ArrayList<>(lessonRoomJoinDao.getLessonsForRoom(room.getId()));
            for (Lesson lesson : lessons) {
                ArrayList<ClassHEIAFR> classes = new ArrayList<>(lessonClassJoinDao.getClassesForLesson(lesson.getId()));
                ArrayList<Teacher> teachers = new ArrayList<>(lessonTeacherJoinDao.getTeachersForLesson(lesson.getId()));
                lessonsCompletes.get(lesson.getDayOfTheWeek()).add(new LessonOfRoomComplete(lesson, classes, teachers));
            }
            lessonsByRoom.put(room, lessonsCompletes);
        }
    }

    private void getRoomsFromInternet(ArrayList<RoomHEIAFR> rooms)
            throws HttpBasicClient.HttpException, InterruptedIOException, HttpBasicClient.NoInternetConnectionException, UnknownHostException {
        ArrayList<Lesson> allLessons = new ArrayList<>(lessonDao.getLessons());
        for (RoomHEIAFR room : rooms) {
            HashMap<Integer, ArrayList<LessonOfRoomComplete>> lessonsCompletes = new HashMap<>();
            for (int i = 0; i < NB_DAYS; i++)
                lessonsCompletes.put(i, new ArrayList<LessonOfRoomComplete>());

            List<LessonOfRoomIncomplete> listReceived = httpBasicClient.getRoomLessons(room.getId());
            if (listReceived != null) {
                ArrayList<LessonOfRoomIncomplete> lessonsIncompletes = new ArrayList<>(listReceived);
                for (LessonOfRoomIncomplete lessonIncomplete : lessonsIncompletes) {
                    Lesson lesson = lessonIncomplete.getLesson();
                    if (allLessons.contains(lesson)) {
                        lesson = allLessons.get(allLessons.indexOf(lesson));
                    } else {
                        long id = lessonDao.insertLesson(lesson);
                        lesson.setId(id);
                        allLessons.add(lesson);
                    }
                    try {
                        lessonRoomJoinDao.insertLessonRoomJoin(new LessonRoomJoin(lesson.getId(), room.getId()));
                    } catch (SQLiteConstraintException e) {
                        // do nothing, the association is allready in the DB
                    }

                    ArrayList<ClassHEIAFR> classes = new ArrayList<>();
                    for (String className : lessonIncomplete.getClassIDs()) {
                        ClassHEIAFR classHEIAFR = classHEIAFRDao.getClass(className);
                        if (classHEIAFR == null) {
                            classHEIAFR = new ClassHEIAFR(className);
                            classHEIAFRDao.insertClass(classHEIAFR);
                        }
                        classes.add(classHEIAFR);
                        try {
                            lessonClassJoinDao.insertLessonClassJoin(new LessonClassJoin(lesson.getId(), className));
                        } catch (SQLiteConstraintException e) {
                            // do nothing, the association is allready in the DB
                        }
                    }

                    ArrayList<Teacher> teachers = new ArrayList<>();
                    for (String teacherAbbr : lessonIncomplete.getTeachersAbbrs()) {
                        Teacher teacher = teacherDao.getTeacher(teacherAbbr);
                        if (teacher == null) {
                            teacher = httpBasicClient.getTeacherInfos(teacherAbbr);
                            teacherDao.insertTeacher(teacher);
                        }
                        teachers.add(teacher);
                        try {
                            lessonTeacherJoinDao.insertLessonTeacherJoin(new LessonTeacherJoin(lesson.getId(), teacherAbbr));
                        } catch (SQLiteConstraintException e) {
                            // do nothing, the association is allready in the DB
                        }
                    }

                    lessonsCompletes.get(lesson.getDayOfTheWeek()).add(new LessonOfRoomComplete(lesson, classes, teachers));
                }
            }
            lessonsByRoom.put(room, lessonsCompletes);
        }
        prefManager.setLastUpdateRoomsLessons(new Date());
    }

    private void updateRoomFreeList(int day, int hourStart, int minutesStart, int hourEnd, int minutesEnd) {
        freeRooms.clear();
        for (RoomHEIAFR room : lessonsByRoom.keySet()) {
            boolean roomFree = true;
            for (LessonOfRoomComplete lessonOfRoomComplete : lessonsByRoom.get(room).get(day)) {
                Lesson lesson = lessonOfRoomComplete.getLesson();

                int thisHourStart = Integer.parseInt(lesson.getTimeStart().substring(0, 2));
                int thisMinutesStart = Integer.parseInt(lesson.getTimeStart().substring(3, 5));
                int thisHourEnd = Integer.parseInt(lesson.getTimeEnd().substring(0, 2));
                int thisMinutesEnd = Integer.parseInt(lesson.getTimeEnd().substring(3, 5));

                if ((hourStart < thisHourEnd || (hourStart == thisHourEnd && minutesStart < thisMinutesEnd)) &&
                        (hourEnd > thisHourStart || (hourEnd == thisHourStart && minutesEnd >= thisMinutesStart))) {
                    roomFree = false;
                    break;
                }
            }
            if (roomFree)
                freeRooms.add(room);
        }
        Collections.sort(freeRooms, RoomHEIAFR.ROOM_HEIAFR_COMPARATOR);
    }

    private void showHttpErrorCode(int httpCode) {
        String message;
        switch (httpCode) {
            case CODE_NO_PROBLEMS:
                return;
            case 401:
                message = getResources().getString(R.string.login_failed);
                break;
            case CODE_NO_CONNECTION:
                message = getResources().getString(R.string.unknown_host);
                break;
            default:
                message = getResources().getString(R.string.http_error) + " " + httpCode;
                break;
        }
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        if (httpCode == 401) launchWelcomeScreen();
    }

    private void launchWelcomeScreen() {
        prefManager.setUserDisconnected(true);
        prefManager.setPassword("");
        startActivity(new Intent(FindRoomActivity.this, WelcomeActivity.class));
        finish();
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_SAVED_ROOMS, freeRooms);
        savedInstanceState.putSerializable(KEY_SAVED_LESSONS_ROOMS, lessonsByRoom);
        savedInstanceState.putInt(KEY_SAVED_DAY_SELECTED, spinnerDay.getSelectedItemPosition());
        savedInstanceState.putInt(KEY_SAVED_HOUR_START, startHour);
        savedInstanceState.putInt(KEY_SAVED_MINUTES_START, startMinutes);
        savedInstanceState.putInt(KEY_SAVED_HOUR_END, endHour);
        savedInstanceState.putInt(KEY_SAVED_MINUTES_END, endMinutes);
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
            savedInstanceState.putBoolean(KEY_SAVED_THREAD_ALIVE, true);
            savedInstanceState.putSerializable(KEY_SAVED_THREAD_TYPE, threadType);
        } else {
            savedInstanceState.putBoolean(KEY_SAVED_THREAD_ALIVE, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bottomSheetDialog != null && bottomSheetDialog.isVisible())
            bottomSheetDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefManager.getLastUpdateRoomsLessons() == null) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.action_find_room))
                    .setMessage(getString(R.string.warning_first_search))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            refreshLessons();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
    }
}
