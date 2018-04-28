package au.com.smarttrace.beacon.service.jobs.firebase;

import android.content.Context;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

public class Dispatcher {
    private FirebaseJobDispatcher dispatcher;

    private static Dispatcher instance = null;
    public static Dispatcher getInstance(Context context) {
        if (instance == null) {
            instance = new Dispatcher(context);
        }
        return instance;
    }

    public Dispatcher(Context context) {
        dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
    }

    public void schedule() {
        Job job = dispatcher.newJobBuilder()
                .setService(MyJobService.class)
                .setTag("run-back-ground")
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(5, 30))
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                )
                .build();
        dispatcher.mustSchedule(job);
    }
}
