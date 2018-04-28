package au.com.smarttrace.beacon;

import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;

import au.com.smarttrace.beacon.service.NetworkUtils;

public class FireLogger {
    public static void d(String message) {
        FirebaseDatabase.getInstance().getReference("logs").child(NetworkUtils.getGatewayId()).child((new Date()).toString()).setValue(message);
    }

}
