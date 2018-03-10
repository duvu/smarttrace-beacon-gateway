package com.TZONE.Bluetooth.Temperature.Listener;

/**
 * Firmware Upgrade Service Callback Interface
 * Created by Forrest on 2017/4/17.
 */
public interface IOTAService {
    /**
     * Complete
     * @param status
     */
    public abstract void OnComplete(boolean status);
    /**
     * Progress
     * @param progress
     */
    public abstract void OnProgress(int progress, int step);

    /**
     * Write file speed
     * @param speed
     */
    public abstract void OnWriteSpeed(double speed);

    /**
     * Write file progress
     * @param i
     * @param total
     */
    public abstract void OnWriteProgress(int i, int total);
    /**
     * error
     * 1 = Not connected to the device
     * 2 = The device does not support OTA functionality
     * 3 = Can not enter upgrade mode
     * 4 = Unable to receive directed broadcasts
     * 5 = In upgrade mode, device information can not be obtained
     * 6 = The password is not correct
     * 7 = Unable to get device information
     * 8 = Password is error
     * 9 = Upgrade mode initialization failed
     * 10 = Write file size process exception
     * 11 = The upgrade package is corrupted
     * 12 = Write file size, device not responding
     * 13 = Write file size, rejected by device
     * 14 = Inform the device to receive the initialization parameters related to the process of abnormal
     * 15 = Inform the device to receive the initialization parameters, the device did not respond
     * 16 = Write initialization process exception
     * 17 = Write initialization, device not responding
     * 18 = Notify the device to write the file initialization parameters related to the completion of the sending process exception
     * 19 = Notify the device to write the file initialization parameters related to the completion of sending, the device did not respond
     * 20 = Notify the device to write the file initialization parameters related to the completion of transmission, the device refused
     * 21 = Set the device to receive an N packet waiting for an exception during device response
     * 22 = Set the device to receive N packets waiting for device response, the device is not responding
     * 23 = Notification device ready to receive file process exception
     * 24 = Inform the device ready to receive the file, the device did not respond
     * 25 = Subcontract send data process exception
     * 26 = Subcontract to send data, the device is not responding
     * 27 = Subcontracting data, data errors, or device unreachable
     * 28 = Check if the check of the received file is correct
     * 29 = Check that the file is checked for correctness and the device is not responding
     * 30 = Check that the verification of the received file is correct and the error is ended
     * 31 = Start a new firmware process exception
     * 32 = Start new firmware, device not responding
     * 33 = Check for new firmware process exceptions
     * 34 = Start EnableCCCD failed
     * @param code
     */
    public abstract void OnError(int code);
}
