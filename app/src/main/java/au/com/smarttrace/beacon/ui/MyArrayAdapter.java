package au.com.smarttrace.beacon.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.TZONE.Bluetooth.Utils.MeasuringDistance;
import com.TZONE.Bluetooth.Utils.StringUtil;
import com.TZONE.Bluetooth.Utils.TemperatureUnitUtil;

import java.util.Date;
import java.util.List;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.model.AdvancedDevice;

/**
 * Created by beou on 3/9/18.
 */

public class MyArrayAdapter extends ArrayAdapter {
    private Context context;
    private List<Device> deviceList;
    private int resourceId;

    public MyArrayAdapter(@NonNull Context context, int resource, List<Device> deviceList) {
        super(context, resource);
        this.context = context;
        this.resourceId = resource;
        this.deviceList = deviceList;
    }

    @Override
    public int getCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return deviceList != null ? deviceList.get(position) : null;
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
            viewHolder.txtRSSI = (TextView) convertView.findViewById(R.id.txtRSSI);
            viewHolder.txtDistance = (TextView) convertView.findViewById(R.id.txtDistance);
            viewHolder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            viewHolder.txtMajor = (TextView) convertView.findViewById(R.id.txtMajor);
            viewHolder.txtMinor = (TextView) convertView.findViewById(R.id.txtMinor);
            viewHolder.txtUrl = (TextView) convertView.findViewById(R.id.txtUrl);
            viewHolder.txtMacAddress = (TextView) convertView.findViewById(R.id.txtMacAddress);
            viewHolder.txtProtocol = (TextView) convertView.findViewById(R.id.txtProtocol);
            viewHolder.txtSN = (TextView) convertView.findViewById(R.id.txtSN);
            viewHolder.btnDetail = (ImageView) convertView.findViewById(R.id.btnDetail);
            viewHolder.imgRssi = (ImageView) convertView.findViewById(R.id.imgRssi);
            viewHolder.imgBattery = (ImageView) convertView.findViewById(R.id.imgBattery);
            viewHolder.txtBattery = (TextView) convertView.findViewById(R.id.txtBattery);
            viewHolder.layoutIbeacon = (LinearLayout) convertView.findViewById(R.id.layoutIbeacon);
            viewHolder.layoutEddystone = (LinearLayout) convertView.findViewById(R.id.layoutEddystone);
            viewHolder.layoutTemperature = (LinearLayout) convertView.findViewById(R.id.layoutTemperature);
            viewHolder.txtTemperature = (TextView) convertView.findViewById(R.id.txtTemperature);
            viewHolder.txtHumidity = (TextView) convertView.findViewById(R.id.txtHumidity);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Device device = (Device) getItem(position);
        viewHolder.layoutTemperature.setVisibility(View.VISIBLE);
        viewHolder.layoutIbeacon.setVisibility(View.GONE);
        viewHolder.layoutEddystone.setVisibility(View.GONE);
        if (device != null) {
            int rssi = device.RSSI;
            viewHolder.txtRSSI.setText("rssi:" + rssi + " dBm");

            int measuredPower = -60;
            double distance = MeasuringDistance.calculateAccuracy(measuredPower, rssi);
            viewHolder.txtDistance.setText("" + distance + "m  " + measuredPower + "");

            String strName = "";
            if (device.Name == null || device.Name.equals(""))
                strName = "--";
            else
                strName = device.Name;
            viewHolder.txtName.setText(strName);
            viewHolder.txtMacAddress.setText(device.MacAddress);

            if (device.HardwareModel.equals("3901"))
                viewHolder.txtProtocol.setText("BT04 (v" + device.Firmware + ")");
            else if (device.HardwareModel.equals("3C01"))
                viewHolder.txtProtocol.setText("BT04B (v" + device.Firmware + ")");
            else if (device.HardwareModel.equals("3A01"))
                viewHolder.txtProtocol.setText("BT05 (v" + device.Firmware + ")");
            else if (device.HardwareModel.equals("3A04"))
                viewHolder.txtProtocol.setText("BT05B (v" + device.Firmware + ")");
            else
                viewHolder.txtProtocol.setText(device.HardwareModel + " (v" + device.Firmware + ")");

            String sn = device.SN;
            viewHolder.txtSN.setText("--");
            if (sn != null && !sn.isEmpty()) {
                viewHolder.txtSN.setText(sn);
            }
            // RSSI
            if (rssi > -60) {
                viewHolder.imgRssi.setImageResource(R.drawable.rssi_5);
            } else if (rssi > -70) {
                viewHolder.imgRssi.setImageResource(R.drawable.rssi_4);
            } else if (rssi > -80) {
                viewHolder.imgRssi.setImageResource(R.drawable.rssi_3);
            } else if (rssi > -90) {
                viewHolder.imgRssi.setImageResource(R.drawable.rssi_2);
            } else {
                viewHolder.imgRssi.setImageResource(R.drawable.rssi_1);
            }

            // More than 1 minute is not scanned to be offline
            Date now = new Date();
            long TotalTime = (now.getTime() - device.LastScanTime.getTime())
                    / (1000);
            if (TotalTime > 60) {
                convertView.setBackgroundColor(Color.parseColor("#AFCCCCCC"));
            } else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }

            int battery = device.Battery;
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
            if (device.Temperature != -1000) {
                //viewHolder.txtTemperature.setText(d.Temperature + " ℃ | " + (int) ((d.Temperature + 273.15) * 100) / 100.00 + "K");
                //viewHolder.txtTemperature.setText(d.Temperature+" ℃ | "+(int)((d.Temperature*1.8+32)*100)/100.00+"℉");
                viewHolder.txtTemperature.setText(new TemperatureUnitUtil(device.Temperature).GetStringTemperature(AppConfig.TemperatureUnit));
            }
            viewHolder.txtHumidity.setText("--");
            if (device.Humidity != -1000)
                viewHolder.txtHumidity.setText(StringUtil.ToString(device.Humidity, 1) + " %");

            if (device.AlarmType.equals("80") || device.AlarmType.equals("40") || device.AlarmType.equals("C0")) {
                //convertView.setBackgroundColor(Color.parseColor("#AFAE0000"));
                if (device.AlarmType.equals("40") || device.AlarmType.equals("C0")) {
                    viewHolder.txtTemperature.setTextColor(Color.RED);
                }
                if (device.AlarmType.equals("80") || device.AlarmType.equals("C0")) {
                    viewHolder.txtBattery.setTextColor(Color.RED);
                }
            } else {
                viewHolder.txtTemperature.setTextColor(Color.parseColor("#808080"));
                viewHolder.txtBattery.setTextColor(Color.parseColor("#808080"));
            }
        }
        return convertView;
    }

    public final class ViewHolder {
        public TextView txtName;
        public TextView txtMacAddress;
        public TextView txtRSSI;
        public ImageView imgRssi;
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
        public TextView txtHumidity;

        public ImageView btnDetail;
    }

    public void setDeviceList(List<Device> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }
}
