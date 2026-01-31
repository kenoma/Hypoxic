package com.lanius.kenoma.pob.hr_monitor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HxMData
{
    final public byte STX;
    final public byte HxM;
    final public byte DLC;

    final public int FirmwareID;
    final public int FirmwareVer;
    final public int HardwareID;
    final public int HardwareVer;
    final public int BatteryCharge;
    final public int HeartRate;
    final public int HeartBeatNum;

    final public int[] HeartBeatTimestamp = new int[15];
    final public int Reserv_1;
    final public int Reserv_2;
    final public int Reserv_3;
    final public int Distance;
    final public int InstSpeed;
    final public int Strides;
    final public byte Reserv_4;
    final public int Reserv_5;
    final public byte CRC;
    final public byte ETX;

    public HxMData(int[] _HeartBeatTimestamp, int _HeartBeatNum)
    {
        int hr = 0;
        int count = 0;
        for (int i = 0; i < 15; i++)
        {
            HeartBeatTimestamp[i] = _HeartBeatTimestamp[i];
            if (i != 0 && HeartBeatTimestamp[i] != 0 && HeartBeatTimestamp[i - 1] != 0)
            {
                int elapsed = HeartBeatTimestamp[i - 1] - HeartBeatTimestamp[i];
                if(elapsed>300 && elapsed<3000)
                {
                    hr+=elapsed;
                    count++;
                }
            }
        }
        HeartBeatNum = _HeartBeatNum;
        if (count != 0 && hr != 0)
            HeartRate = 60000 / (hr / count);
        else
            HeartRate = 1;
        STX = 0;
        HxM = 0;
        DLC = 0;
        FirmwareID = 0;
        FirmwareVer = 0;
        HardwareID = 0;
        HardwareVer = 0;
        BatteryCharge = 100;
        Reserv_1 = 0;
        Reserv_2 = 0;
        Reserv_3 = 0;
        Distance = 0;
        InstSpeed = 0;
        Strides = 0;
        Reserv_4 = 0;
        Reserv_5 = 0;
        CRC = 0;
        ETX = 0;
    }

    public HxMData(byte[] bytes)
    {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN); // or LITTLE_ENDIAN
        STX = bb.get();
        HxM = bb.get();
        DLC = bb.get();
        FirmwareID = ToUnsignedInt(bb.getShort());
        FirmwareVer = ToUnsignedInt(bb.getShort());
        HardwareID = ToUnsignedInt(bb.getShort());
        HardwareVer = ToUnsignedInt(bb.getShort());
        BatteryCharge = ToUnsignedInt(bb.get());
        HeartRate = ToUnsignedInt(bb.get());
        HeartBeatNum = ToUnsignedInt(bb.get());
        for (int i = 0; i < 15; i++)
            HeartBeatTimestamp[i] = ToUnsignedInt(bb.getShort());
        Reserv_1 = bb.getShort();
        Reserv_2 = bb.getShort();
        Reserv_3 = bb.getShort();
        Distance = ToUnsignedInt(bb.getShort());
        InstSpeed = ToUnsignedInt(bb.getShort());
        Strides = ToUnsignedInt(bb.get());
        Reserv_4 = bb.get();
        Reserv_5 = bb.getShort();
        CRC = bb.get();
        ETX = bb.get();
    }

    public static int ToUnsignedInt(byte b)
    {
        return b & 0xff;
    }

    public static int ToUnsignedInt(short b)
    {
        return b & 0xffff;
    }

    public int RR(int t)
    {
        if (t + 1 < HeartBeatTimestamp.length)
        {
            if (HeartBeatTimestamp[t + 1] < HeartBeatTimestamp[t])
                return HeartBeatTimestamp[t] - HeartBeatTimestamp[t + 1];
            else
                return (65535 - HeartBeatTimestamp[t + 1]) + HeartBeatTimestamp[t];
        }
        return 0;
    }
}
