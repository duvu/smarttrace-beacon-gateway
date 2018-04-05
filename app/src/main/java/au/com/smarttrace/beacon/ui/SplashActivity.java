package au.com.smarttrace.beacon.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.net.Http;
import au.com.smarttrace.beacon.net.model.LoginResponse;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashActivity extends AppCompatActivity {
    private static final boolean AUTO_HIDE = true;
    private UserLoginTask mAuthTask = null;

    private static final int UI_ANIMATION_DELAY = 100;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mMoveToLogin = new Runnable() {
        @Override
        public void run() {
            moveToLogin();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        mContentView = findViewById(R.id.fullscreen_content);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLogin();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void moveToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private void moveToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private void checkLogin() {
        String username = SharedPref.getUserName();
        String password = SharedPref.getPassword();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            mHideHandler.removeCallbacks(mMoveToLogin);
            mHideHandler.postDelayed(mMoveToLogin, 1000);
        } else {
            //try to login
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    @SuppressLint("StaticFieldLeak")
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String url = AppConfig.WEB_SERVICE_URL + "/login?email=" + mEmail + "&password="+mPassword;
            try {
                LoginResponse response = Http.getIntance().get(url, LoginResponse.class);
                if (response.getStatus().getCode() != 0) {
                    return false;
                }
                //-- start store login data
                // get more data and store
                storeUserData(response);
            } catch (IOException e) {
                moveToLogin();
                finish();
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                moveToMain();
                finish();
            } else {
                moveToLogin();
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }

        private void storeUserData(LoginResponse data) {
            SharedPref.saveToken(data.getResponse().getToken());
            SharedPref.saveExpiredStr(data.getResponse().getExpired());
            SharedPref.saveTokenInstance(data.getResponse().getInstance());
        }
    }
}
