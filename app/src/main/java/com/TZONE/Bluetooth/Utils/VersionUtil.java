package com.TZONE.Bluetooth.Utils;

import android.os.Handler;
import android.os.Message;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Forrest on 2015/6/25.
 */
public class VersionUtil {
    private String url="http://development.tzonedigital.cn/sensor/temperature/android/app_publish.xml";
    public static final int PARSESUCCWSS=0x2001;
    public static final int PARSEFAIL=0x2000;
    private Handler handler;
    public VersionUtil(Handler handler) {
        this.handler=handler;
    }
    /**
     * For XML on the network
     */
    public void getXml(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    HttpURLConnection conn=(HttpURLConnection)new
                            URL(url).openConnection();
                    // Set the connection timeout
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode()==200) {
                        InputStream inputStream=conn.getInputStream();
                        List<NetVersion> list=pullXml(inputStream);
                        if (list.size()>0) {
                            // If the resolution is not empty, the parsed data is sent to the UI thread
                            Message msg=new Message();
                            msg.obj=list;
                            msg.what=PARSESUCCWSS;
                            handler.sendMessage(msg);
                            return;
                        }
                    }
                } catch (Exception e) {}
                Message msg=new Message();
                msg.obj=null;
                msg.what=PARSEFAIL;
                handler.sendMessage(msg);
            }
        }).start();
    }
    /**
     * Xml parsing and encapsulated into it
     * @param inputStream
     */
    protected List<NetVersion> pullXml(InputStream inputStream) {
        List<NetVersion> list=new ArrayList<NetVersion>();
        try {
            XmlPullParser pullParser= Xml.newPullParser();
            NetVersion netVersion=null;
            pullParser.setInput(inputStream, "utf-8");
            int eventCode=pullParser.getEventType();
            while (eventCode!= XmlPullParser.END_DOCUMENT) {
                String targetName=pullParser.getName();
                switch (eventCode) {
                    case XmlPullParser.START_TAG:
                        if ("Version".equals(targetName)) {
                            // Handles the Start node of the Version
                            netVersion =new NetVersion();
                            netVersion.ID = pullParser.getAttributeValue(0);
                        }else if ("PublishDate".equals(targetName)) {
                            netVersion.PublishDate = pullParser.nextText();
                        }else if("BT04".equals(targetName)){
                            netVersion.OTA_BT04_Version = pullParser.getAttributeValue(0);
                        }else if("BT05".equals(targetName)){
                            netVersion.OTA_BT05_Version = pullParser.getAttributeValue(0);
                        }else if("BT04B".equals(targetName)){
                            netVersion.OTA_BT04B_Version = pullParser.getAttributeValue(0);
                        }else if("BT05B".equals(targetName)){
                            netVersion.OTA_BT05B_Version = pullParser.getAttributeValue(0);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("Version".equals(targetName)) {
                            // Handles the end node of the Version
                            list.add(netVersion);
                        }
                        break;
                }
                // Parse the next node (start node, end node)
                eventCode=pullParser.next();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return list;
    }

    public class NetVersion{
        /**
         * Version
         */
        public String ID;
        /**
         * PublishDate
         */
        public String PublishDate;
        /**
         * BT04 OTA Version
         */
        public String OTA_BT04_Version;
        /**
         * BT05 OTA Version
         */
        public String OTA_BT05_Version;
        /**
         * BT04B OTA Version
         */
        public String OTA_BT04B_Version;
        /**
         * BT05B OTA Version
         */
        public String OTA_BT05B_Version;
    }
}
