package ch.lburgy.heiafrschedule.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.List;

import ch.lburgy.heiafrschedule.R;
import ch.lburgy.heiafrschedule.database.ClassHEIAFR;
import ch.lburgy.heiafrschedule.database.ClassHEIAFRDao;
import ch.lburgy.heiafrschedule.database.MyDatabase;
import ch.lburgy.heiafrschedule.http.HttpBasicClient;
import ch.lburgy.heiafrschedule.preferences.PrefManager;
import ch.lburgy.heiafrschedule.selfupdate.SelfUpdate;
import ch.lburgy.heiafrschedule.ui.ViewPagerDisable;

public class WelcomeActivity extends AppCompatActivity {

    private static final int FIRST_INDEX_RADIO_GROUP = 1;
    private static final int CODE_NO_CONNECTION = -2;

    private ViewPagerDisable viewPager;
    private LinearLayout dotsLayout;
    private int[] layouts;
    private ImageButton btnPrev, btnNext;
    private TextInputEditText txtUsername, txtPassword;
    private PrefManager prefManager;
    private boolean loggedIn;
    private AutofillManager afm;
    private HttpBasicClient httpBasicClient;
    private MyDatabase myDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PrefManager(this);
        AppCompatDelegate.setDefaultNightMode(prefManager.getTheme());
        if (!prefManager.isLaunchWelcome() && !prefManager.isUserDisconnected())
            launchHomeScreen();

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            SelfUpdate.checkUpdate(this, "https://api.github.com/repos/burgyl/HEIA-FR-Schedule/releases/latest");

        httpBasicClient = new HttpBasicClient(this);
        myDatabase = MyDatabase.getInstance(this);

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_welcome);

        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        // layouts of all welcome sliders
        // add few more layouts if you want
        if (prefManager.isUserDisconnected()) {
            layouts = new int[]{
                    R.layout.welcome_slide_1,
            };
        } else {
            layouts = new int[]{
                    R.layout.welcome_slide_1,
                    R.layout.welcome_slide_2,
                    R.layout.welcome_slide_3,
            };
        }

        // adding bottom dots
        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();

        Space statusBar = findViewById(R.id.status_bar);
        statusBar.setLayoutParams(new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight()
        ));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            afm = getSystemService(AutofillManager.class);
        }

        MyViewPagerAdapter myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setEnable(false);
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() == layouts.length - 1 && !prefManager.isUserDisconnected()) {
                    // only if it's the first time. If the user was disconnected, it's taken care of when the login is successful
                    prefManager.setLaunchWelcome(false);
                    launchHomeScreen();
                } else {
                    nextPage();
                }
            }
        });

        setNextEnable(false);

        View root = findViewById(R.id.activity_welcome_root);
        setupUI(root);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void addBottomDots(int currentPage) {
        TextView[] dots = new TextView[layouts.length];

        int colorsActive = getResources().getColor(R.color.colorWelcomeAccent);
        int colorsInactive = getResources().getColor(R.color.colorWelcomeAccentDark);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive);
    }

    private void nextPage() {
        int nextPage = viewPager.getCurrentItem() + 1;
        switch (nextPage) {
            case 1:
                checkLoginGetClasses(txtUsername.getText().toString(), txtPassword.getText().toString(), nextPage);
                break;
            case 2:
                RadioGroup radioGroupClassesID = findViewById(R.id.radio_group_classes_id);
                int selectedIndex = radioGroupClassesID.getCheckedRadioButtonId() - FIRST_INDEX_RADIO_GROUP;
                RadioButton radioButton = (RadioButton) radioGroupClassesID.getChildAt(selectedIndex);
                String classID = radioButton.getText().toString();
                prefManager.setClassID(classID);
                viewPager.setCurrentItem(nextPage);
                break;
            default:
                viewPager.setCurrentItem(nextPage);
                break;
        }
    }

    private void checkLoginGetClasses(final String username, final String password, final int nextPage) {
        if (loggedIn) {
            viewPager.setCurrentItem(nextPage);
        } else if (username != null && !"".equals(username) && password != null && !"".equals(password)) {
            String progressTitle = getResources().getString(R.string.progress_login_title);
            final ProgressDialog progressDialog = ProgressDialog.show(this, progressTitle, null, true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        httpBasicClient.setCredentials(username, password);
                        List<ClassHEIAFR> classes = null;
                        int httpCode = -1;
                        try {
                            classes = httpBasicClient.getClasses();
                            ClassHEIAFRDao classHEIAFRDao = myDatabase.getClassHEIAFRDao();
                            classHEIAFRDao.deleteClasses();
                            classHEIAFRDao.insertClasses(classes);
                        } catch (HttpBasicClient.HttpException e) {
                            httpCode = e.getCode();
                        } catch (UnknownHostException | HttpBasicClient.NoInternetConnectionException e) {
                            httpCode = CODE_NO_CONNECTION;
                        }

                        final List<ClassHEIAFR> finalClasses = classes;
                        final int finalHttpCode = httpCode;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();

                                loggedIn = finalHttpCode == -1;
                                afterCheckedLogin(username, password, nextPage, finalClasses, finalHttpCode);
                            }
                        });
                    } catch (InterruptedIOException e) {
                        // do nothing
                    }
                }
            }).start();
        }
    }

    private void afterCheckedLogin(String username, String password, int nextPage, List<ClassHEIAFR> classes, int httpCode) {
        if (loggedIn) {
            prefManager.setUsername(username);
            prefManager.setPassword(password);

            if (prefManager.isUserDisconnected()) {
                prefManager.setUserDisconnected(false);
                launchHomeScreen();
            }

            RadioGroup radioGroupClassesID = findViewById(R.id.radio_group_classes_id);
            if (radioGroupClassesID.getChildCount() == 0) {
                LayoutInflater inflater = LayoutInflater.from(this);

                for (ClassHEIAFR classHEIAFR : classes) {
                    RadioButton radioButton = (RadioButton) inflater.inflate(R.layout.radio_button, null);
                    radioButton.setText(classHEIAFR.getName());
                    radioGroupClassesID.addView(radioButton);
                }
                String classID = prefManager.getClassID();
                if (classID != null && !"".equals(classID)) {
                    for (int i = 0; i < radioGroupClassesID.getChildCount(); i++) {
                        RadioButton radioButton = (RadioButton) radioGroupClassesID.getChildAt(i);
                        if (classID.equals(radioButton.getText().toString())) {
                            radioGroupClassesID.check(FIRST_INDEX_RADIO_GROUP + i);
                            break;
                        }
                    }
                } else {
                    radioGroupClassesID.check(FIRST_INDEX_RADIO_GROUP);
                }
            }

            viewPager.setEnable(true);
            viewPager.setCurrentItem(nextPage);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                afm.commit();
        } else {
            String message;
            if (httpCode == 401)
                message = getResources().getString(R.string.login_failed);
            else if (httpCode == CODE_NO_CONNECTION)
                message = getResources().getString(R.string.unknown_host);
            else
                message = getResources().getString(R.string.http_error) + " " + httpCode;

            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void setNextEnable(boolean enable) {
        btnNext.setEnabled(enable);
        if (enable) {
            btnNext.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_next));
        } else {
            btnNext.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_next_disable));
        }
    }

    private void launchHomeScreen() {
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        finish();
    }

    //  viewpager change listener
    private final ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            if (position == 0) {
                // first page
                btnPrev.setVisibility(View.GONE);
                btnNext.setVisibility(View.VISIBLE);
            } else {
                // other pages
                btnPrev.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void setupUI(View view) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    looseFocus();
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    private void looseFocus() {
        View view = this.getCurrentFocus();
        if (view == null) return;
        InputMethodManager inputMethodManager = (InputMethodManager)
                this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        this.getCurrentFocus().clearFocus();
    }

    public class LoginTextWatch implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            loggedIn = false;
            viewPager.setEnable(false);
            if (!"".equals(txtUsername.getText().toString()) && !"".equals(txtPassword.getText().toString())) {
                setNextEnable(true);
            } else {
                setNextEnable(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    public class MyViewPagerAdapter extends PagerAdapter {

        public MyViewPagerAdapter() {
        }

        @NotNull
        @Override
        public Object instantiateItem(@NotNull ViewGroup container, int position) {
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);
            if (position == 0) {
                txtUsername = view.findViewById(R.id.txt_username);
                txtPassword = view.findViewById(R.id.txt_password);
                LoginTextWatch loginTextWatch = new LoginTextWatch();
                txtUsername.addTextChangedListener(loginTextWatch);
                txtPassword.addTextChangedListener(loginTextWatch);
                txtUsername.setText(prefManager.getUsername());
                txtPassword.setText(prefManager.getPassword());
            }
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
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
}