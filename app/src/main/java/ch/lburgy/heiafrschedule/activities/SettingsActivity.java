package ch.lburgy.heiafrschedule.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.List;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.ClassHEIAFR;
import ch.lburgy.heiafrschedule.database.ClassHEIAFRDao;
import ch.lburgy.heiafrschedule.database.LessonDao;
import ch.lburgy.heiafrschedule.database.MyDatabase;
import ch.lburgy.heiafrschedule.database.RoomHEIAFRDao;
import ch.lburgy.heiafrschedule.database.TeacherDao;
import ch.lburgy.heiafrschedule.http.HttpBasicClient;
import ch.lburgy.heiafrschedule.preferences.PrefManager;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String keyTheme9;
    private String keyTheme10;
    private String keyClass;
    private String keyExtraClassChanged;

    private PrefManager prefManager;
    private SharedPreferences prefs;
    private static boolean classChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        keyTheme9 = getResources().getString(R.string.settings_key_theme_9);
        keyTheme10 = getResources().getString(R.string.settings_key_theme_10);
        keyClass = getResources().getString(R.string.settings_key_class);
        keyExtraClassChanged = getResources().getString(R.string.key_extra_class_changed);

        prefManager = new PrefManager(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        Intent i = getIntent();
        i.putExtra(keyExtraClassChanged, classChanged);
        setResult(RESULT_OK, i);
        super.finish();
    }

    @Override
    protected void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (keyTheme9.equals(key)) {
            String themeString9 = sharedPreferences.getString(keyTheme9, "MODE_NIGHT_AUTO_BATTERY");
            changeTheme(themeString9);
        } else if (keyTheme10.equals(key)) {
            String themeString10 = sharedPreferences.getString(keyTheme10, "MODE_NIGHT_FOLLOW_SYSTEM");
            changeTheme(themeString10);
        } else if (keyClass.equals(key)) {
            String classChoosen = sharedPreferences.getString(keyClass, "");
            if (!classChoosen.equals(prefManager.getClassID())) {
                prefManager.setClassID(classChoosen);
                classChanged = true;
            }
        }
    }

    private void changeTheme(String themeString) {
        int theme = -1;
        switch (themeString) {
            case "MODE_NIGHT_FOLLOW_SYSTEM":
                theme = MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            case "MODE_NIGHT_AUTO_BATTERY":
                theme = MODE_NIGHT_AUTO_BATTERY;
                break;
            case "MODE_NIGHT_NO":
                theme = MODE_NIGHT_NO;
                break;
            case "MODE_NIGHT_YES":
                theme = MODE_NIGHT_YES;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(theme);
        prefManager.setTheme(theme);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private static final int CODE_NO_CONNECTION = -2;

        private PrefManager prefManager;
        private HttpBasicClient httpBasicClient;
        private LessonDao lessonDao;
        private ClassHEIAFRDao classHEIAFRDao;
        private RoomHEIAFRDao roomHEIAFRDao;
        private TeacherDao teacherDao;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            prefManager = new PrefManager(getContext());
            httpBasicClient = new HttpBasicClient(getContext(), prefManager.getUsername(), prefManager.getPassword());
            MyDatabase myDatabase = MyDatabase.getInstance(getContext());
            lessonDao = myDatabase.getLessonDao();
            classHEIAFRDao = myDatabase.getClassHEIAFRDao();
            roomHEIAFRDao = myDatabase.getRoomHEIAFRDao();
            teacherDao = myDatabase.getTeacherDao();

            String keyTheme9 = getResources().getString(R.string.settings_key_theme_9);
            String keyTheme10 = getResources().getString(R.string.settings_key_theme_10);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                findPreference(keyTheme10).setVisible(true);
            } else {
                findPreference(keyTheme9).setVisible(true);
            }

            Preference btnDeleteDatas = findPreference(getString(R.string.settings_key_delete_datas));
            btnDeleteDatas.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    deleteDatas();
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_see_github)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/burgyl/HEIA-FR-Schedule"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_license_httpcomponents)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://hc.apache.org/"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_license_jsoup)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://jsoup.org/"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_license_moshi)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/square/moshi"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_license_okhttp)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/square/okhttp"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_license_okio)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/square/okio"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            findPreference(getString(R.string.settings_key_license_selfupdatingapp)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/burgyl/SelfUpdatingApp"));
                    startActivity(browserIntent);
                    return true;
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<ClassHEIAFR> classes = classHEIAFRDao.getClasses();
                    updateClasses(classes);
                }
            }).start();
        }

        private void deleteDatas() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    lessonDao.deleteLessons();
                    roomHEIAFRDao.deleteRooms();
                    teacherDao.deleteTeachers();
                    prefManager.setLastUpdateRoomsLessons(null);
                    classChanged = true;
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            String message = getResources().getString(R.string.datas_deleted);
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                    refreshClasses();
                }
            }).start();
        }

        private void refreshClasses() {
            try {
                int httpCode = -1;
                try {
                    List<ClassHEIAFR> classes = httpBasicClient.getClasses();
                    classHEIAFRDao.deleteClasses();
                    classHEIAFRDao.insertClasses(classes);
                    updateClasses(classes);
                } catch (HttpBasicClient.HttpException e) {
                    httpCode = e.getCode();
                } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                    httpCode = CODE_NO_CONNECTION;
                }

                final int finalHttpCode = httpCode;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        final String message;
                        switch (finalHttpCode) {
                            case -1:
                                message = getResources().getString(R.string.classes_refreshed);
                                break;
                            case 401:
                                message = getResources().getString(R.string.login_failed);
                                break;
                            case CODE_NO_CONNECTION:
                                message = getResources().getString(R.string.unknown_host);
                                break;
                            default:
                                message = getResources().getString(R.string.http_error) + " " + finalHttpCode;
                                break;
                        }

                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        if (finalHttpCode == 401) launchWelcomeScreen();
                    }
                });
            } catch (InterruptedIOException e) {
                // do nothing
            }
        }

        private void launchWelcomeScreen() {
            prefManager.setUserDisconnected(true);
            prefManager.setPassword("");
            startActivity(new Intent(getActivity(), WelcomeActivity.class));
            getActivity().finish();
        }

        private void updateClasses(List<ClassHEIAFR> classes) {
            String[] classesString = new String[classes.size()];
            for (int i = 0; i < classes.size(); i++)
                classesString[i] = classes.get(i).getName();

            String keyClass = getResources().getString(R.string.settings_key_class);
            ListPreference prefClass = findPreference(keyClass);
            prefClass.setEntries(classesString);
            prefClass.setEntryValues(classesString);
            prefClass.setValue(prefManager.getClassID());
        }
    }
}