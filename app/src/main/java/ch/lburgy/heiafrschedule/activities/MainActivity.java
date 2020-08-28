package ch.lburgy.heiafrschedule.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.ClassHEIAFR;
import ch.lburgy.heiafrschedule.database.ClassHEIAFRDao;
import ch.lburgy.heiafrschedule.database.Lesson;
import ch.lburgy.heiafrschedule.database.LessonClassJoin;
import ch.lburgy.heiafrschedule.database.LessonClassJoinDao;
import ch.lburgy.heiafrschedule.database.LessonOfClassComplete;
import ch.lburgy.heiafrschedule.database.LessonDao;
import ch.lburgy.heiafrschedule.database.LessonOfClassIncomplete;
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
import ch.lburgy.heiafrschedule.selfupdate.SelfUpdate;
import ch.lburgy.heiafrschedule.ui.BottomSheetDialogLessonsInfos;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String KEY_SAVED_LESSONS = "lessons";
    private static final String KEY_SAVED_THREAD_ALIVE = "thread_alive";
    private static final String KEY_SAVED_THREAD_TYPE = "thread_type";

    private static final int REQUEST_CODE_SETTINGS = 1;
    private static final int REQUEST_CODE_FIND_ROOM = 2;

    private static final int CODE_NO_PROBLEMS = -1;
    private static final int CODE_NO_CONNECTION = -2;

    private static final int NB_DAYS = 5;

    private LessonDao lessonDao;
    private RoomHEIAFRDao roomHEIAFRDao;
    private LessonRoomJoinDao lessonRoomJoinDao;
    private TeacherDao teacherDao;
    private LessonTeacherJoinDao lessonTeacherJoinDao;
    private ClassHEIAFRDao classHEIAFRDao;
    private LessonClassJoinDao lessonClassJoinDao;
    private ClassHEIAFR classHEIAFR;

    private PrefManager prefManager;
    private HttpBasicClient httpBasicClient;

    private ProgressDialog progressDialog;
    private ViewPager viewPager;
    private BottomNavigationView navigation;
    private MyViewPagerAdapter myViewPagerAdapter;
    private BottomSheetDialogLessonsInfos bottomSheetDialog;

    private HashMap<Integer, ArrayList<LessonOfClassComplete>> lessonsCompletesByDay;
    private int dayDisplayed = -1;

    private Thread currentThread;
    private ThreadType threadType;

    enum ThreadType {
        getLessons,
        getLessonsFromInternet;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            SelfUpdate.checkUpdate(this, "https://api.github.com/repos/burgyl/HEIA-FR-Schedule/releases/latest");

        MyDatabase myDatabase = MyDatabase.getInstance(this);
        lessonDao = myDatabase.getLessonDao();
        roomHEIAFRDao = myDatabase.getRoomHEIAFRDao();
        lessonRoomJoinDao = myDatabase.getLessonRoomJoinDao();
        teacherDao = myDatabase.getTeacherDao();
        lessonTeacherJoinDao = myDatabase.getLessonTeacherJoinDao();
        classHEIAFRDao = myDatabase.getClassHEIAFRDao();
        lessonClassJoinDao = myDatabase.getLessonClassJoinDao();
        prefManager = new PrefManager(this);
        AppCompatDelegate.setDefaultNightMode(prefManager.getTheme());
        httpBasicClient = new HttpBasicClient(this, prefManager.getUsername(), prefManager.getPassword());

        navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                classHEIAFR = classHEIAFRDao.getClass(prefManager.getClassID());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(getResources().getString(R.string.app_name) + " " + classHEIAFR.getName());
                    }
                });
            }
        }).start();

        if (savedInstanceState == null) {
            getLessonsAndShowThem(false);
        } else {
            lessonsCompletesByDay = (HashMap<Integer, ArrayList<LessonOfClassComplete>>)
                    savedInstanceState.getSerializable(KEY_SAVED_LESSONS);
            if (savedInstanceState.getBoolean(KEY_SAVED_THREAD_ALIVE, false)) {
                threadType = (ThreadType) savedInstanceState.getSerializable(KEY_SAVED_THREAD_TYPE);
                switch (threadType) {
                    case getLessons:
                        getLessonsAndShowThem(false);
                        break;
                    case getLessonsFromInternet:
                        getLessonsAndShowThem(true);
                        break;
                }
            } else {
                showLessonsStart();
            }
        }
    }

    private void getLessonsAndShowThem(final boolean forceFromInternet) {
        if (MainActivity.this.isDestroyed()) return;
        String progressTitle = getResources().getString(R.string.progress_refresh_lessons_title);
        progressDialog = ProgressDialog.show(this, progressTitle, null, true);
        threadType = ThreadType.getLessons;
        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int httpCode = CODE_NO_PROBLEMS;
                    if (!forceFromInternet) {
                        lessonsCompletesByDay = new HashMap<>();
                        for (int i = 0; i < NB_DAYS; i++)
                            lessonsCompletesByDay.put(i, new ArrayList<LessonOfClassComplete>());

                        if (classHEIAFR == null)
                            classHEIAFR = classHEIAFRDao.getClass(prefManager.getClassID());

                        if (classHEIAFR.getLastUpdate() != null) {
                            List<Lesson> lessons = lessonClassJoinDao.getLessonsForClass(classHEIAFR.getName());
                            for (Lesson lesson : lessons) {
                                List<RoomHEIAFR> rooms = lessonRoomJoinDao.getRoomsForLesson(lesson.getId());
                                List<Teacher> teachers = lessonTeacherJoinDao.getTeachersForLesson(lesson.getId());
                                lessonsCompletesByDay.get(lesson.getDayOfTheWeek()).add(new LessonOfClassComplete(lesson, rooms, teachers));
                            }
                        } else {
                            try {
                                getLessonsFromInternet(false);
                            } catch (HttpBasicClient.HttpException e) {
                                httpCode = e.getCode();
                            } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                                httpCode = CODE_NO_CONNECTION;
                            }
                        }
                    } else {
                        try {
                            getLessonsFromInternet(false);
                        } catch (HttpBasicClient.HttpException e) {
                            httpCode = e.getCode();
                        } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                            httpCode = CODE_NO_CONNECTION;
                        }
                    }

                    final int finalHttpCode = httpCode;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (finalHttpCode == CODE_NO_PROBLEMS) {
                                if (dayDisplayed == -1) {
                                    showLessonsStart();
                                } else {
                                    viewPager.setAdapter(myViewPagerAdapter);
                                    viewPager.setCurrentItem(dayDisplayed);
                                }
                            } else {
                                showHttpErrorCode(finalHttpCode);
                            }

                            if (!MainActivity.this.isDestroyed() && progressDialog != null && progressDialog.isShowing())
                                progressDialog.dismiss();
                        }
                    });
                } catch (InterruptedIOException e) {
                    // do nothing
                }
            }
        });
        currentThread.start();
    }

    private void showLessonsStart() {
        viewPager = findViewById(R.id.view_pager);
        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        int dayNumber = 0;
        switch (day) {
            case Calendar.TUESDAY:
                dayNumber = 1;
                break;
            case Calendar.WEDNESDAY:
                dayNumber = 2;
                break;
            case Calendar.THURSDAY:
                dayNumber = 3;
                break;
            case Calendar.FRIDAY:
                dayNumber = 4;
                break;
            case Calendar.SATURDAY:
                dayNumber = 5;
                break;
            case Calendar.SUNDAY:
                dayNumber = 6;
                break;
        }

        if (lessonsCompletesByDay.containsKey(dayNumber)) {
            ArrayList<LessonOfClassComplete> lessonsOfTheDay = lessonsCompletesByDay.get(dayNumber);
            if (lessonsOfTheDay.size() > 0) {
                LessonOfClassComplete lastLesson = lessonsOfTheDay.get(lessonsOfTheDay.size() - 1);
                String timeEnd = lastLesson.getLesson().getTimeEnd();
                int lastHour = Integer.parseInt(timeEnd.substring(0, 2));
                int minutesOfTheLastHour = Integer.parseInt(timeEnd.substring(3, 5));
                if (hour > lastHour || (hour == lastHour && minutes >= minutesOfTheLastHour))
                    dayNumber++;
            }
        }
        if (dayNumber > 4)
            dayNumber = 0;

        dayDisplayed = dayNumber;
        viewPager.setCurrentItem(dayDisplayed);
    }

    private void refreshLessons() {
        String progressTitle = getResources().getString(R.string.progress_refresh_lessons_title);
        progressDialog = ProgressDialog.show(this, progressTitle, null, true);
        threadType = ThreadType.getLessonsFromInternet;
        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int httpCode = CODE_NO_PROBLEMS;
                    if (httpBasicClient.isConnectedToInternet()) {
                        try {
                            getLessonsFromInternet(true);
                        } catch (HttpBasicClient.HttpException e) {
                            httpCode = e.getCode();
                        } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                            httpCode = CODE_NO_CONNECTION;
                        }
                    } else {
                        httpCode = CODE_NO_CONNECTION;
                    }

                    final int finalHttpCode = httpCode;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (finalHttpCode == CODE_NO_PROBLEMS) {
                                viewPager.setAdapter(myViewPagerAdapter);
                                viewPager.setCurrentItem(dayDisplayed);
                            } else {
                                showHttpErrorCode(finalHttpCode);
                            }
                            if (!MainActivity.this.isDestroyed() && progressDialog != null && progressDialog.isShowing())
                                progressDialog.dismiss();
                        }
                    });
                } catch (InterruptedIOException e) {
                    // do nothing
                }
            }
        });
        currentThread.start();
    }

    private void getLessonsFromInternet(boolean deleteExisting) throws HttpBasicClient.HttpException, UnknownHostException, HttpBasicClient.NoInternetConnectionException, InterruptedIOException {
        ArrayList<LessonOfClassIncomplete> lessonsIncompletes = new ArrayList<>(httpBasicClient.getClassLessons(classHEIAFR.getName()));
        if (deleteExisting) {
            lessonClassJoinDao.deleteLessonsFromClass(classHEIAFR.getName());
            lessonClassJoinDao.deleteLessonsNoClass();
        }

        if (roomHEIAFRDao.getNbRoom() == 0) {
            ArrayList<RoomHEIAFR> rooms = new ArrayList<>(httpBasicClient.getRooms());
            roomHEIAFRDao.insertRooms(rooms);
        }

        lessonsCompletesByDay = new HashMap<>();
        for (int i = 0; i < NB_DAYS; i++)
            lessonsCompletesByDay.put(i, new ArrayList<LessonOfClassComplete>());

        ArrayList<Lesson> allLessons = new ArrayList<>(lessonDao.getLessons());
        for (LessonOfClassIncomplete lessonIncomplete : lessonsIncompletes) {
            Lesson lesson = lessonIncomplete.getLesson();
            if (allLessons.contains(lesson)) {
                lesson = allLessons.get(allLessons.indexOf(lesson));
            } else {
                long id = lessonDao.insertLesson(lesson);
                lesson.setId(id);
                allLessons.add(lesson);
            }
            try {
                lessonClassJoinDao.insertLessonClassJoin(new LessonClassJoin(lesson.getId(), classHEIAFR.getName()));
            } catch (SQLiteConstraintException e) {
                // do nothing, the association is allready in the DB
            }

            ArrayList<RoomHEIAFR> rooms = new ArrayList<>();
            for (String roomID : lessonIncomplete.getRoomsIDs()) {
                RoomHEIAFR room = roomHEIAFRDao.getRoom(roomID);
                if (room == null) {
                    room = new RoomHEIAFR(roomID);
                    roomHEIAFRDao.insertRoom(room);
                }
                rooms.add(room);
                try {
                    lessonRoomJoinDao.insertLessonRoomJoin(new LessonRoomJoin(lesson.getId(), roomID));
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

            lessonsCompletesByDay.get(lesson.getDayOfTheWeek()).add(new LessonOfClassComplete(lesson, rooms, teachers));
        }
        classHEIAFR.setLastUpdate(new Date());
        classHEIAFRDao.updateClassHEIAFR(classHEIAFR);
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
        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_SETTINGS);
                return true;
            case R.id.action_refresh_lessons:
                refreshLessons();
                return true;
            case R.id.action_find_room:
                startActivityForResult(new Intent(this, FindRoomActivity.class), REQUEST_CODE_FIND_ROOM);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (data == null) return;
            String keyExtraClassChanged = getResources().getString(R.string.key_extra_class_changed);
            boolean classChanged = data.getBooleanExtra(keyExtraClassChanged, false);
            if (classChanged) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        classHEIAFR = classHEIAFRDao.getClass(prefManager.getClassID());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTitle(getResources().getString(R.string.app_name) + " " + classHEIAFR.getName());
                            }
                        });
                    }
                }).start();
                getLessonsAndShowThem(false);
            }
        }
        if (prefManager.isLaunchWelcome()) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_day_0:
                dayDisplayed = 0;
                break;
            case R.id.navigation_day_1:
                dayDisplayed = 1;
                break;
            case R.id.navigation_day_2:
                dayDisplayed = 2;
                break;
            case R.id.navigation_day_3:
                dayDisplayed = 3;
                break;
            case R.id.navigation_day_4:
                dayDisplayed = 4;
                break;
        }
        viewPager.setCurrentItem(dayDisplayed);
        return true;
    }

    private final ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            dayDisplayed = position;
            navigation.getMenu().getItem(position).setChecked(true);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    public class MyViewPagerAdapter extends PagerAdapter {

        public MyViewPagerAdapter() {
        }

        @NotNull
        @Override
        public Object instantiateItem(@NotNull ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.main_slide, container, false);
            container.addView(view);

            putLessonCards(view, inflater, position);

            return view;
        }

        @Override
        public int getCount() {
            return NB_DAYS;
        }

        @Override
        public boolean isViewFromObject(@NotNull View view, @NotNull Object obj) {
            return view == obj;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NotNull Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    private void putLessonCards(View view, LayoutInflater inflater, int position) {
        LinearLayout linearLayout = view.findViewById(R.id.contentLayout);
        linearLayout.removeAllViews();
        for (final LessonOfClassComplete lessonOfClassComplete : lessonsCompletesByDay.get(position)) {
            LinearLayout cardView = (LinearLayout) inflater.inflate(R.layout.card_lesson, null);

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLessonInfos(lessonOfClassComplete);
                }
            });

            TextView txtTimeStart = cardView.findViewById(R.id.time_start);
            TextView txtTimeEnd = cardView.findViewById(R.id.time_end);
            TextView txtName = cardView.findViewById(R.id.name);
            TextView txtRoom = cardView.findViewById(R.id.room);

            txtTimeStart.setText(lessonOfClassComplete.getLesson().getTimeStart());
            txtTimeEnd.setText(lessonOfClassComplete.getLesson().getTimeEnd());
            txtName.setText(lessonOfClassComplete.getLesson().getName());

            StringBuilder roomText = new StringBuilder();
            for (int i = 0; i < lessonOfClassComplete.getRooms().size(); i++) {
                if (i != 0)
                    roomText.append("\n");
                roomText.append(lessonOfClassComplete.getRooms().get(i).getId());
            }
            txtRoom.setText(roomText);

            linearLayout.addView(cardView);
        }
    }

    private void showLessonInfos(LessonOfClassComplete lessonOfClassComplete) {
        bottomSheetDialog = new BottomSheetDialogLessonsInfos(lessonOfClassComplete);
        bottomSheetDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
        bottomSheetDialog.show(getSupportFragmentManager(), bottomSheetDialog.getTag());
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_SAVED_LESSONS, lessonsCompletesByDay);
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
}
