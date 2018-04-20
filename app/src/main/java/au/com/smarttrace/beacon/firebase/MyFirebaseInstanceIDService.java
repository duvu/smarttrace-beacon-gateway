package au.com.smarttrace.beacon.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Logger.d("[firebase] refreshedToken: " + refreshedToken);
        FirebaseDatabase.getInstance().getReference(AppConfig.GATEWAY_ID).setValue(refreshedToken);
    }

}
