package au.com.smarttrace.beacon.net;

public interface Callback {
    public void onResponse(Response response);
    public void onFailed();
}
