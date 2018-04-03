package au.com.smarttrace.beacon;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class UpdateManager extends AsyncTask<String, Integer, Boolean> {
    @Override
    protected Boolean doInBackground(String... strings) {
        Boolean flag = false;
        try {
            URL url = new URL(strings[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
