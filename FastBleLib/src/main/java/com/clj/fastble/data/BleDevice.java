package com.clj.fastble.data;


import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;


public class BleDevice implements Parcelable {

    private BluetoothDevice mDevice;
    private byte[] mScanRecord;
    private int mRssi;
    private long mTimestampNanos;
    private boolean isLoading;
    private int mCount;
    private int power;
    //串口名称
    private UsbSerialDriver mUsbSerialDriver;

    public BleDevice(UsbSerialDriver device) {
        mUsbSerialDriver = device;
    }

    private boolean isConnected;

    public BleDevice(BluetoothDevice device) {
        mDevice = device;
    }

    public BleDevice(BluetoothDevice device, int rssi, byte[] scanRecord, long timestampNanos, int count) {
        mDevice = device;
        mScanRecord = scanRecord;
        mRssi = rssi;
        mTimestampNanos = timestampNanos;
        mCount = count;
    }

    protected BleDevice(Parcel in) {
        mDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        mScanRecord = in.createByteArray();
        mRssi = in.readInt();
        mTimestampNanos = in.readLong();
        mCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDevice, flags);
        dest.writeByteArray(mScanRecord);
        dest.writeInt(mRssi);
        dest.writeLong(mTimestampNanos);
        dest.writeInt(mCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BleDevice> CREATOR = new Creator<BleDevice>() {
        @Override
        public BleDevice createFromParcel(Parcel in) {
            return new BleDevice(in);
        }

        @Override
        public BleDevice[] newArray(int size) {
            return new BleDevice[size];
        }
    };

    public UsbSerialDriver getmUsbSerialDriver() {
        return mUsbSerialDriver;
    }

    public void setmUsbSerialDriver(UsbSerialDriver mUsbSerialDriver) {
        this.mUsbSerialDriver = mUsbSerialDriver;
    }

    public String getName() {
        if (mDevice != null) {
            return mDevice.getName();
        }
        return null;
    }

    public String getMac() {
        if (mDevice != null) {
            return mDevice.getAddress();
        }
        return null;
    }

    public String getKey() {
        if (mDevice != null) {
            return mDevice.getName() + mDevice.getAddress();
        }
        return "";
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    public byte[] getScanRecord() {
        return mScanRecord;
    }

    public void setScanRecord(byte[] scanRecord) {
        this.mScanRecord = scanRecord;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    public long getTimestampNanos() {
        return mTimestampNanos;
    }

    public void setTimestampNanos(long timestampNanos) {
        this.mTimestampNanos = timestampNanos;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    public int getCount() {
        return mCount;
    }

    public void setCount(int mCount) {
        this.mCount = mCount;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
