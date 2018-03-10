package au.com.smarttrace.beacon.ui;

import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

import au.com.smarttrace.beacon.model.ForeGroundEvent;

/**
 * Created by beou on 3/9/18.
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onPause() {
        EventBus.getDefault().post(new ForeGroundEvent(true));
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().post(new ForeGroundEvent(false));
    }
}
