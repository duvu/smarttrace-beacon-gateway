package com.TZONE.Bluetooth.Utils;

import java.text.DecimalFormat;

/**
 * Measured distance
 * Created by Forrest on 2015/4/13.
 */
public class MeasuringDistance {
    /**
     * Get the distance
     * @param mPower
     * @param rssi
     * @return
     */
    public static double calculateAccuracy(int mPower, double rssi) {
        try{
            double distance = 0;
            if (rssi == 0) {
                distance = -1.0;
            }
            double ratio = rssi * 1.0 / mPower;
            if (ratio < 1.0) {
                distance = Math.pow(ratio, 10);
            } else {
                double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
                distance = accuracy;
            }
            // Keep two decimal places
            DecimalFormat df = new DecimalFormat("#.00");
            return Double.valueOf(df.format(distance));
        }catch (Exception ex){
            return -1;
        }
    }
}
