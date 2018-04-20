package com.TZONE.Bluetooth;

import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Read, write, deal with class
 */
public class ConfigHandle{
    public HashMap<String,Boolean> Configs;
    private boolean isRequestComplete;
    public ConfigHandle(){
        if(Configs == null)
            Configs = new HashMap<String,Boolean>();
        isRequestComplete = false;
    }

    /**
     * Request
     * @param uuid
     */
    public void ConfigRequest(String uuid){
        uuid = uuid.replace("-","");
        Configs.put(uuid,false);
    }

    /**
     * Respond
     * @param uuid
     */
    public void ConfigRespond(String uuid){
        uuid = uuid.replace("-","");
        if (Configs.containsKey(uuid))
            Configs.put(uuid,true);
    }

    public void ConfigRequestComplete(){
        isRequestComplete = true;
    }

    /**
     * Check if there is a response
     * @param uuid
     * @return
     */
    public boolean IsRespond(String uuid){
        uuid = uuid.replace("-","");
        Iterator iter = Configs.entrySet().iterator();
        while (iter.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            String key = (String) entry.getKey();
            boolean val = (boolean) entry.getValue();
            if (key.equals(uuid) && val) {
               return true;
            }
        }
        return false;
    }

    /**
     * Is Complete
     */
    public boolean IsComplete(){
        if(!isRequestComplete)
            return false;

        Iterator iter = Configs.entrySet().iterator();
        while (iter.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            boolean val = (boolean) entry.getValue();
            if (!val) {
                Log.i("IsComplete", (String) entry.getKey() + " ---> false");
                return false;
            }
        }
        return true;
    }
}
