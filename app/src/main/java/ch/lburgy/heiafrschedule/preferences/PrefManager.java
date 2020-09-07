package ch.lburgy.heiafrschedule.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import java.util.Date;

import ch.lburgy.heiafrschedule.type_converter.DateConverter;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

public class PrefManager {
    private static final int PRIVATE_MODE = 0;
    private static final String PREF_NAME = "ch.lburgy.heiafrschedule";

    private static final String KEY_LAUNCH_WELCOME = "launch_welcome";
    private static final String KEY_USER_DISCONNECTED = "user_disconnected";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_CLASS_ID = "class_id";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LAST_UPDATE_ROOMS_LESSONS = "last_update_rooms_lessons";
    private static final String KEY_SCHEDULE_DATE_1 = "schedule_date_1";
    private static final String KEY_SCHEDULE_DATE_2 = "schedule_date_2";

    private static final String KEY_IV = "-iv";
    private static final String KEY_ENCRYPTED = "encrypted_key";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    private Cryptography cryptography;
    private CryptographyRSAAES cryptographyRSAAES;

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                cryptography = new Cryptography();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                cryptographyRSAAES = new CryptographyRSAAES(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setLaunchWelcome(boolean launchWelcome) {
        editor.putBoolean(KEY_LAUNCH_WELCOME, launchWelcome);
        editor.commit();
    }

    public boolean isLaunchWelcome() {
        return pref.getBoolean(KEY_LAUNCH_WELCOME, true);
    }

    public void setUserDisconnected(boolean userDisconnected) {
        editor.putBoolean(KEY_USER_DISCONNECTED, userDisconnected);
        editor.commit();
    }

    public boolean isUserDisconnected() {
        return pref.getBoolean(KEY_USER_DISCONNECTED, false);
    }

    public void setUsername(String username) {
        try {
            encrypt(KEY_USERNAME, username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        try {
            return getEncrypted(KEY_USERNAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setPassword(String password) {
        try {
            encrypt(KEY_PASSWORD, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPassword() {
        try {
            return getEncrypted(KEY_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setLastUpdateRoomsLessons(Date lastUpdate) {
        editor.putLong(KEY_LAST_UPDATE_ROOMS_LESSONS, DateConverter.fromDate(lastUpdate));
        editor.commit();
    }

    public Date getLastUpdateRoomsLessons() {
        return DateConverter.toDate(pref.getLong(KEY_LAST_UPDATE_ROOMS_LESSONS, -1));
    }

    public void setScheduleDates(Date[] scheduleDates) {
        if (scheduleDates != null && scheduleDates.length == 2) {
            editor.putLong(KEY_SCHEDULE_DATE_1, DateConverter.fromDate(scheduleDates[0]));
            editor.putLong(KEY_SCHEDULE_DATE_2, DateConverter.fromDate(scheduleDates[1]));
            editor.commit();
        }
    }

    public Date[] getScheduleDates() {
        Date[] scheduleDates = new Date[2];
        scheduleDates[0] = DateConverter.toDate(pref.getLong(KEY_SCHEDULE_DATE_1, -1));
        scheduleDates[1] = DateConverter.toDate(pref.getLong(KEY_SCHEDULE_DATE_2, -1));
        return scheduleDates;
    }

    public void setTheme(int theme) {
        editor.putInt(KEY_THEME, theme);
        editor.commit();
    }

    public int getTheme() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return pref.getInt(KEY_THEME, MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            return pref.getInt(KEY_THEME, MODE_NIGHT_AUTO_BATTERY);
        }
    }

    public void setClassID(String classID) {
        editor.putString(KEY_CLASS_ID, classID);
        editor.commit();
    }

    public String getClassID() {
        return pref.getString(KEY_CLASS_ID, "");
    }

    private void encrypt(String key, String toEncrypt) throws Exception {
        String encrypted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            byte[] bytes = cryptography.encryptText(key, toEncrypt);
            encrypted = Base64.encodeToString(bytes, Base64.DEFAULT);
            String iv = Base64.encodeToString(cryptography.getIv(), Base64.DEFAULT);
            editor.putString(key + KEY_IV, iv);
        } else {
            String encryptedKeyB64 = getKeyEncrypted();
            byte[] bytes = toEncrypt.getBytes("UTF-8");
            encrypted = cryptographyRSAAES.encrypt(encryptedKeyB64, bytes);
        }
        editor.putString(key, encrypted);
        editor.commit();
    }

    private String getEncrypted(String key) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String encrypte = pref.getString(key, null);
            String ivString = pref.getString(key + KEY_IV, null);
            if (encrypte == null || ivString == null) return null;
            byte[] iv = Base64.decode(ivString, Base64.DEFAULT);
            byte[] bytes = Base64.decode(encrypte, Base64.DEFAULT);
            return cryptography.decryptData(key, bytes, iv);
        } else {
            String encryptedKeyB64 = getKeyEncrypted();
            String encrypted = pref.getString(key, null);
            byte[] bytes = Base64.decode(encrypted, Base64.DEFAULT);
            bytes = cryptographyRSAAES.decrypt(encryptedKeyB64, bytes);
            return new String(bytes, "UTF-8");
        }
    }

    private String getKeyEncrypted() throws Exception {
        String encryptedKeyB64 = pref.getString(KEY_ENCRYPTED, null);
        if (encryptedKeyB64 != null) return encryptedKeyB64;
        encryptedKeyB64 = cryptographyRSAAES.generateKey();
        editor.putString(KEY_ENCRYPTED, encryptedKeyB64);
        editor.commit();
        return encryptedKeyB64;
    }
}