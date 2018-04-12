package au.com.smarttrace.beacon.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.TZONE.Bluetooth.Temperature.Model.BT04Package;
//import com.TZONE.Bluetooth.Utils.MeasuringDistance;
//import com.TZONE.Bluetooth.Utils.StringUtil;
//import com.TZONE.Bluetooth.Utils.TemperatureUnitUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.model.BT04Package;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by beou on 3/9/18.
 */

public class BeaconAdapter extends ArrayAdapter {
    private Context context;
    private List<BT04Package> dataPackageList;
    private int resourceId;

    public BeaconAdapter(@NonNull Context context, int resource, List<BT04Package> dataPackageList) {
        super(context, resource);
        this.context = context;
        this.resourceId = resource;
        if (dataPackageList != null) {
            Collections.sort(dataPackageList, new Comparator<BT04Package>() {
                @Override
                public int compare(BT04Package o1, BT04Package o2) {
                    return o1.getDistance().compareTo(o2.getDistance());
                }
            });
        }
        this.dataPackageList = dataPackageList;
    }

    @Override
    public int getCount() {
        return dataPackageList != null ? dataPackageList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return dataPackageList != null ? dataPackageList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView==null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.control_scan_device_list, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.txtDistance = (TextView) convertView.findViewById(R.id.txtDistance);
            //viewHolder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            viewHolder.txtMajor = (TextView) convertView.findViewById(R.id.txtMajor);
            viewHolder.txtMinor = (TextView) convertView.findViewById(R.id.txtMinor);
            viewHolder.txtUrl = (TextView) convertView.findViewById(R.id.txtUrl);
            viewHolder.txtTime = (TextView) convertView.findViewById(R.id.txtScannedTime);
            viewHolder.txtProtocol = (TextView) convertView.findViewById(R.id.txtProtocol);
            viewHolder.txtSN = (TextView) convertView.findViewById(R.id.txtSN);
            //viewHolder.imgRssi = (ImageView) convertView.findViewById(R.id.imgRssi);
            viewHolder.imgBattery = (ImageView) convertView.findViewById(R.id.imgBattery);
            viewHolder.txtBattery = (TextView) convertView.findViewById(R.id.txtBattery);
            viewHolder.layoutIbeacon = (LinearLayout) convertView.findViewById(R.id.layoutIbeacon);
            viewHolder.layoutEddystone = (LinearLayout) convertView.findViewById(R.id.layoutEddystone);
            viewHolder.layoutTemperature = (LinearLayout) convertView.findViewById(R.id.layoutTemperature);
            viewHolder.txtTemperature = (TextView) convertView.findViewById(R.id.txtTemperature);
//            viewHolder.txtHumidity = (TextView) convertView.findViewById(R.id.txtHumidity);
            viewHolder.btnPair = (Button) convertView.findViewById(R.id.btn_pair);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final BT04Package beaconData = (BT04Package) getItem(position);
        viewHolder.layoutTemperature.setVisibility(View.VISIBLE);
        viewHolder.layoutIbeacon.setVisibility(View.GONE);
        viewHolder.layoutEddystone.setVisibility(View.GONE);
        if (beaconData != null) {
            int rssi = beaconData.getRssi();
//            viewHolder.txtRSSI.setText("rssi:" + rssi + " dBm");

            double distance = beaconData.getDistance();
            viewHolder.txtDistance.setText(formatDistance(distance));

            String strName = "";
            if (beaconData.getName() == null || beaconData.getName().equals(""))
                strName = "--";
            else
                strName = beaconData.getName();
//            viewHolder.txtName.setText(strName);
            //viewHolder.txtTime.setText(beaconData.getBluetoothAddress());
            viewHolder.txtTime.setText(DataUtil.formatDate(beaconData.getTimestamp(), "hh:mma", TimeZone.getDefault()));

//            if (beaconData.getModel().equals("3901"))
//                viewHolder.txtProtocol.setText("BT04 (v" + beaconData.getFirmware() + ")");
//            else if (beaconData.getModel().equals("3C01"))
//                viewHolder.txtProtocol.setText("BT04B (v" + beaconData.getFirmware() + ")");
//            else if (beaconData.getModel().equals("3A01"))
//                viewHolder.txtProtocol.setText("BT05 (v" + beaconData.getFirmware() + ")");
//            else if (beaconData.getModel().equals("3A04"))
//                viewHolder.txtProtocol.setText("BT05B (v" + beaconData.getFirmware() + ")");
//            else
//                viewHolder.txtProtocol.setText(beaconData.getModel() + " (v" + beaconData.getFirmware() + ")");

            if (beaconData.getModel().equals("3901"))
                viewHolder.txtProtocol.setText("BT04");
            else if (beaconData.getModel().equals("3C01"))
                viewHolder.txtProtocol.setText("BT04B");
            else if (beaconData.getModel().equals("3A01"))
                viewHolder.txtProtocol.setText("BT05");
            else if (beaconData.getModel().equals("3A04"))
                viewHolder.txtProtocol.setText("BT05B");
            else
                viewHolder.txtProtocol.setText(beaconData.getModel());


            String sn = beaconData.getSerialNumber();
            viewHolder.txtSN.setText("--");
            if (sn != null && !sn.isEmpty()) {
                viewHolder.txtSN.setText(sn);
            }
//            // RSSI
//            if (rssi > -60) {
//                viewHolder.imgRssi.setImageResource(R.drawable.rssi_5);
//            } else if (rssi > -70) {
//                viewHolder.imgRssi.setImageResource(R.drawable.rssi_4);
//            } else if (rssi > -80) {
//                viewHolder.imgRssi.setImageResource(R.drawable.rssi_3);
//            } else if (rssi > -90) {
//                viewHolder.imgRssi.setImageResource(R.drawable.rssi_2);
//            } else {
//                viewHolder.imgRssi.setImageResource(R.drawable.rssi_1);
//            }

            // More than 1 minute is not scanned to be offline
            Date now = new Date();
            long TotalTime = (now.getTime() - beaconData.getTimestamp()) / (1000);
            if (TotalTime > 60) {
                Logger.d("Switching dead");
                convertView.setBackgroundColor(Color.parseColor("#AFCCCCCC"));
            } else {
                Logger.d("Switching live");
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }

            int battery = beaconData.getBatteryLevel();
            if (battery < 20) {
                viewHolder.imgBattery.setImageResource(R.drawable.battery_00);
            } else if (battery < 40) {
                viewHolder.imgBattery.setImageResource(R.drawable.battery_20);
            } else if (battery < 60) {
                viewHolder.imgBattery.setImageResource(R.drawable.battery_40);
            } else if (battery < 80) {
                viewHolder.imgBattery.setImageResource(R.drawable.battery_60);
            } else if (battery < 100) {
                viewHolder.imgBattery.setImageResource(R.drawable.battery_80);
            } else {
                viewHolder.imgBattery.setImageResource(R.drawable.battery_100);
            }
            if (battery >= 0) {
                viewHolder.txtBattery.setText("" + battery + "%");
                viewHolder.imgBattery.setVisibility(View.VISIBLE);
                viewHolder.txtBattery.setVisibility(View.VISIBLE);
            } else {
                viewHolder.txtBattery.setText("--");
                viewHolder.imgBattery.setVisibility(View.GONE);
                viewHolder.txtBattery.setVisibility(View.GONE);
            }

            viewHolder.txtTemperature.setText("-- ");
            if (beaconData.getTemperature() != -1000) {
                //viewHolder.txtTemperature.setText(d.Temperature + " ℃ | " + (int) ((d.Temperature + 273.15) * 100) / 100.00 + "K");
                //viewHolder.txtTemperature.setText(d.Temperature+" ℃ | "+(int)((d.Temperature*1.8+32)*100)/100.00+"℉");
                //viewHolder.txtTemperature.setText(new TemperatureUnitUtil(beaconData.Temperature).GetStringTemperature(AppConfig.TemperatureUnit));
                viewHolder.txtTemperature.setText(beaconData.getTemperatureString());
            }
//            viewHolder.txtHumidity.setText("--");
//            if (beaconData.getHumidity() != -1000)
//                viewHolder.txtHumidity.setText(beaconData.getHumidity() + " %");

            /*if (beaconData.AlarmType.equals("80") || beaconData.AlarmType.equals("40") || beaconData.AlarmType.equals("C0")) {
                //convertView.setBackgroundColor(Color.parseColor("#AFAE0000"));
                if (beaconData.AlarmType.equals("40") || beaconData.AlarmType.equals("C0")) {
                    viewHolder.txtTemperature.setTextColor(Color.RED);
                }
                if (beaconData.AlarmType.equals("80") || beaconData.AlarmType.equals("C0")) {
                    viewHolder.txtBattery.setTextColor(Color.RED);
                }
            } else*/ {
                viewHolder.txtTemperature.setTextColor(Color.parseColor("#808080"));
                viewHolder.txtBattery.setTextColor(Color.parseColor("#808080"));
            }

            viewHolder.btnPair.setEnabled(!beaconData.isPaired());
            viewHolder.btnPair.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //get webservice for pair
                    doPair(AppConfig.GATEWAY_ID, beaconData.getSerialNumberString());
                }
            });
        }
        return convertView;
    }

    public final class ViewHolder {
        public TextView txtName;
        public TextView txtTime;
//        public ImageView imgRssi;
        public TextView txtDistance;
        public ImageView imgBattery;
        public TextView txtBattery;
        public TextView txtProtocol;
        public TextView txtSN;

        //ibeacon
        public LinearLayout layoutIbeacon;
        public TextView txtUUID;
        public TextView txtMajor;
        public TextView txtMinor;

        //eddystone
        public LinearLayout layoutEddystone;
        public TextView txtUrl;

        //Temperature
        public LinearLayout layoutTemperature;
        public TextView txtTemperature;
//        public TextView txtHumidity;

        public Button btnPair;
    }

    public void setDataPackageList(List<BT04Package> dataPackageList) {
        Logger.d("[BeaconAdapter] + ...setDataPackageList()");
        if (dataPackageList != null) {
            Collections.sort(dataPackageList, new Comparator<BT04Package>() {
                @Override
                public int compare(BT04Package o1, BT04Package o2) {
                    return o1.getDistance().compareTo(o2.getDistance());
                }
            });
        }
        this.dataPackageList = dataPackageList;
        notifyDataSetChanged();
    }

    private String formatDistance(double distance) {
        String units = "m";
        distance+=0.1;
        return String.format(Locale.US, "%.1f", distance) + units;
    }

    private void doPair(String imei, String bSn) {
        WebService.savePairedPhone(imei, bSn, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //
                Logger.d("Paired!" + response.body().string());
            }
        });
    }
}
