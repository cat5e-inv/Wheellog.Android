package com.cooper.wheellog.utils;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.R;
import com.cooper.wheellog.WheelData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import timber.log.Timber;

import android.content.Intent;

import static com.cooper.wheellog.utils.InMotionAdapter.Model.*;

import org.koin.java.KoinJavaComponent;

public class InMotionAdapter extends BaseAdapter {
    private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
    private static InMotionAdapter INSTANCE;
    private Timer keepAliveTimer;
    private int passwordSent = 0;
    private boolean needSlowData = true;
    protected boolean settingCommandReady = false;
    private static int updateStep = 0;
    protected byte[] settingCommand;

    @Override
    public boolean decode(byte[] data) {
        for (byte c : data) {
            if (!unpacker.addChar(c)) {
                continue;
            }
            CANMessage result = CANMessage.verify(unpacker.getBuffer());
            if (result == null) {
                continue;
            }
            // data OK
            CANMessage.IDValue idValue = CANMessage.IDValue.NoOp;
            for (CANMessage.IDValue id: CANMessage.IDValue.values()) {
                if (id.value == result.id) {
                    idValue = id;
                    break;
                }
            }
            switch (idValue) {
                case GetFastInfo:
                    return result.parseFastInfoMessage(model);
                case Alert:
                    return result.parseAlertInfoMessage();
                case GetSlowInfo:
                    if (result.isValid()) {
                        needSlowData = false;
                    }
                    return result.parseSlowInfoMessage();
                case PinCode:
                    passwordSent = Integer.MAX_VALUE;
                    break;
            }

            if (getContext() != null) {
                String news = null;
                switch (idValue) {
                    case Calibration:
                        news = result.data[0] == 1
                                ? getContext().getString(R.string.calibration_success)
                                : getContext().getString(R.string.calibration_fail);
                        break;
                    case RideMode:
                        news = result.data[0] == 1
                                ? getContext().getString(R.string.ridemode_success)
                                : getContext().getString(R.string.ridemode_fail);
                        break;
                    case RemoteControl:
                        news = result.data[0] == 1
                                ? getContext().getString(R.string.remotecontrol_success)
                                : getContext().getString(R.string.remotecontrol_fail);
                        break;
                    case Light:
                        news = result.data[0] == 1
                                ? getContext().getString(R.string.light_success)
                                : getContext().getString(R.string.light_fail);
                        break;
                    case HandleButton:
                        news = result.data[0] == 1
                                ? getContext().getString(R.string.handlebutton_success)
                                : getContext().getString(R.string.handlebutton_fail);
                        break;
                    case SpeakerVolume:
                        news = result.data[0] == 1
                                ? getContext().getString(R.string.speakervolume_success)
                                : getContext().getString(R.string.speakervolume_fail);
                        break;
                }

                if (news != null) {
                    Timber.i("News to send: %s, sending Intent", news);
                    Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                    intent.putExtra(Constants.INTENT_EXTRA_NEWS, news);
                    getContext().sendBroadcast(intent);
                }
            }
        }
        return false;
    }

    @Override
    public boolean isReady() {
        return model != Model.UNKNOWN && !Objects.equals(WheelData.getInstance().getSerial(), "");
    }

    enum Mode {
        rookie(0),
        general(1),
        smoothly(2),
        unBoot(3),
        bldc(4),
        foc(5);

        private final int value;

        Mode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public int getMaxSpeed() {
        switch (model) {
            case V5:
            case V5PLUS:
            case V5F:
            case V5D:
                return 25;
            case V8:
            case Glide3:
                return 35;
            case V8F:
            case V8S:
            case V10S:
            case V10SF:
            case V10:
            case V10F:
            case V10T:
            case V10FT:
                return 45;
        }
        return 70;
    }

    public boolean getLedThere() {
        switch (model) {
            case Glide3:
            case V8:
            case V8F:
            case V8S:
            case V10S:
            case V10SF:
            case V10T:
            case V10:
            case V10F:
            case V10FT:
                return true;
        }
        return false;
    }

    public boolean getWheelModesWheel() {
        switch (model) {
            case V8F:
            case V8S:
            case V10S:
            case V10SF:
            case V10T:
            case V10:
            case V10F:
            case V10FT:
                return true;
        }
        return false;
    }

    public enum Model {
        R1N("0", 3812.0d),
        R1S("1", 1000.0d),
        R1CF("2", 3812.0d),
        R1AP("3", 3812.0d),
        R1EX("4", 3812.0d),
        R1Sample("5", 1000.0d),
        R1T("6", 3810.0d),
        R10("7", 3812.0d),
        V3("10", 3812.0d),
        V3C("11", 3812.0d),
        V3PRO("12", 3812.0d),
        V3S("13", 3812.0d),
        R2N("21", 3812.0d),
        R2S("22", 3812.0d),
        R2Sample("23", 3812.0d),
        R2("20", 3812.0d),
        R2EX("24", 3812.0d),
        R0("30", 1000.0d),
        L6("60", 3812.0d),
        Lively("61", 3812.0d),
        V5("50", 3812.0d),
        V5PLUS("51", 3812.0d),
        V5F("52", 3812.0d),
        V5D("53", 3812.0d),
        V8("80", 3812.0d),
        V8F("86", 3812.0d),
        V8S("87", 3812.0d),
        Glide3("85", 3812.0d),
        V10S("100", 3812.0d),
        V10SF("101", 3812.0d),
        V10("140", 3812.0d),
        V10F("141", 3812.0d),
        V10T("142", 3812.0d),
        V10FT("143", 3812.0d),
        UNKNOWN("x", 3812.0d);

        private final String value;
        private final double speedCalculationFactor;

        Model(String value, double speedCalculationFactor) {
            this.value = value;
            this.speedCalculationFactor = speedCalculationFactor;
        }

        public String getValue() {
            return value;
        }

        public double getSpeedCalculationFactor() {
            return speedCalculationFactor;
        }

        public boolean belongToInputType(String type) {
            if ("0".equals(type)) {
                return value.length() == 1;
            } else return value.substring(0, 1).equals(type) && value.length() == 2;
        }

        public static Model findById(String id) {
            Timber.i("Model %s", id);
            for (Model m : Model.values()) {
                if (m.getValue().equals(id)) return m;
            }
            return Model.UNKNOWN;
        }

        public static Model findByBytes(byte[] data) {
            StringBuilder stringBuffer = new StringBuilder();
            if (data.length >= 108) {
                if (data[107] > (byte) 0) {
                    stringBuffer.append(data[107]);
                }
                stringBuffer.append(data[104]);
            }

            return Model.findById(stringBuffer.toString());
        }
    }

    enum WorkMode {
        idle(0),
        drive(1),
        zero(2),
        largeAngle(3),
        checkc(4),
        lock(5),
        error(6),
        carry(7),
        remoteControl(8),
        shutdown(9),
        pomStop(10),
        unknown(11),
        unlock(12);

        private final int value;

        WorkMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static Model model = Model.UNKNOWN;
    InMotionUnpacker unpacker = new InMotionUnpacker();

    private void setModel(Model value){
        model = value;
    }

    public void startKeepAliveTimer(String password) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    if (passwordSent < 6) {
                        if (WheelData.getInstance().bluetoothCmd(InMotionAdapter.CANMessage.getPassword(password).writeBuffer())) {
                            Timber.i("Sent password message");
                            passwordSent++;
                        } else {
                            updateStep = 5;
                        }

                    } else if (model == UNKNOWN | needSlowData) {
                        if (WheelData.getInstance().bluetoothCmd(InMotionAdapter.CANMessage.getSlowData().writeBuffer())) {
                            Timber.i("Sent infos message");
                        } else {
                            updateStep = 5;
                        }

                    } else if (settingCommandReady) {
                        if (WheelData.getInstance().bluetoothCmd(settingCommand)) {
                            needSlowData = true;
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else {
                            updateStep = 5; // after +1 and %10 = 0
                        }
                    } else {
                        if (!WheelData.getInstance().bluetoothCmd(CANMessage.standardMessage().writeBuffer())) {
                            Timber.i("Unable to send keep-alive message");
                            updateStep = 5;
                        } else {
                            Timber.i("Sent keep-alive message");
                        }
                    }

                }
                updateStep++;
                updateStep %= 10;
                Timber.i("Step: %d", updateStep);
            }
        };
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 200, 25);
    }

    @Override
    public void switchFlashlight() {
        boolean light = !appConfig.getLightEnabled();
        appConfig.setLightEnabled(light);
        setLightState(light);
    }

    @Override
    public void setLightState(final boolean lightEnable) {
        settingCommand = InMotionAdapter.CANMessage.setLight(lightEnable).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setLedState(final boolean ledEnable) {
        settingCommand = InMotionAdapter.CANMessage.setLed(ledEnable).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setHandleButtonState(final boolean handleButtonEnable) {
        settingCommand = InMotionAdapter.CANMessage.setHandleButton(handleButtonEnable).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void updateMaxSpeed(final int maxSpeed) {
        settingCommand = InMotionAdapter.CANMessage.setMaxSpeed(maxSpeed).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setSpeakerVolume(final int speakerVolume) {
        settingCommand = InMotionAdapter.CANMessage.setSpeakerVolume(speakerVolume).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setPedalTilt(final int angle) {
        settingCommand = InMotionAdapter.CANMessage.setTiltHorizon(angle).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setPedalSensivity(final int sensivity) {
        settingCommand = InMotionAdapter.CANMessage.setPedalSensivity(sensivity).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setRideMode(final boolean rideMode) {
        settingCommand = InMotionAdapter.CANMessage.setRideMode(rideMode).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void powerOff() {
        settingCommand = InMotionAdapter.CANMessage.powerOff().writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void wheelCalibration() {
        settingCommand = InMotionAdapter.CANMessage.wheelCalibration().writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void wheelBeep() {
        if (getWheelModesWheel()) settingCommand = InMotionAdapter.CANMessage.wheelBeep().writeBuffer();
        else settingCommand = InMotionAdapter.CANMessage.playSound((byte) 4).writeBuffer(); // old wheels like V8 and V5F don't have beep command, so let's play sound instead
        settingCommandReady = true;
    }

    public void wheelSound(byte soundNumber) {
        settingCommand = InMotionAdapter.CANMessage.playSound(soundNumber).writeBuffer();
        settingCommandReady = true;
    }

    static Mode intToMode(int mode) {
        if ((mode & 16) != 0) {
            return Mode.rookie;
        } else if ((mode & 32) != 0) {
            return Mode.general;
        } else if (((mode & 64) == 0) || ((mode & 128) == 0)) {
            return Mode.unBoot;
        } else {
            return Mode.smoothly;
        }
    }


    static Mode intToModeWithL6(int mode) {
        if ((mode & 15) != 0) {
            return Mode.bldc;
        } else {
            return Mode.foc;
        }
    }

    static WorkMode intToWorkModeWithL6(int mode) {
        if ((mode & 240) != 0) {
            return WorkMode.lock;
        } else {
            return WorkMode.unlock;
        }
    }

    static WorkMode intToWorkMode(int mode) {

        int v = mode & 0xF;

        switch (v) {
            case 0:
                return WorkMode.idle;
            case 1:
                return WorkMode.drive;
            case 2:
                return WorkMode.zero;
            case 3:
                return WorkMode.largeAngle;
            case 4:
                return WorkMode.checkc;
            case 5:
                return WorkMode.lock;
            case 6:
                return WorkMode.error;
            case 7:
                return WorkMode.carry;
            case 8:
                return WorkMode.remoteControl;
            case 9:
                return WorkMode.shutdown;
            case 16:
                return WorkMode.pomStop;
            default:
                return WorkMode.unknown;
        }
    }

    static int batteryFromVoltage(int volts_i, Model model) {
        double volts = (double)volts_i/100.0;
        double batt;
        final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);

        if (model.belongToInputType("1") || model == R0) {
            if (volts >= 82.50) {
                batt = 1.0;
            } else if (volts > 68.0) {
                batt = (volts - 68.0) / 14.50;
            } else {
                batt = 0.0;
            }
        } else {
            Boolean useBetterPercents = appConfig.getUseBetterPercents();
            if (model.belongToInputType("5") || model == Model.V8 || model == Model.Glide3 || model == Model.V8F || model == Model.V8S) {
                if (useBetterPercents) {
                    if (volts > 84.00) {
                        batt = 1.0;
                    } else if (volts > 68.5) {
                        batt = (volts - 68.5) / 15.5;
                    } else {
                        batt = 0.0;
                    }
                } else {
                    if (volts > 82.50) {
                        batt = 1.0;
                    } else if (volts > 68.0) {
                        batt = (volts - 68.0) / 14.5;
                    } else {
                        batt = 0.0;
                    }
                }
            } else if (model == Model.V10 || model == Model.V10F || model == Model.V10S || model == Model.V10SF || model == Model.V10T || model == Model.V10FT) {
                if (useBetterPercents) {
                    if (volts > 83.50) {
                        batt = 1.00;
                    } else if (volts > 68.00) {
                        batt = (volts - 66.50) / 17;
                    } else if (volts > 64.00) {
                        batt = (volts - 64.00) / 45;
                    } else {
                        batt = 0;
                    }
                } else {
                    if (volts > 82.50) {
                        batt = 1.0;
                    } else if (volts > 68.0) {
                        batt = (volts - 68.0) / 14.5;
                    } else {
                        batt = 0.0;
                    }
                }
            } else if (model.belongToInputType("6")) {
                batt = 0.0;
            } else {
                if (volts >= 82.00) {
                    batt = 1.0;
                } else if (volts > 77.8) {
                    batt = ((volts - 77.8) / 4.2) * 0.2 + 0.8;
                } else if (volts > 74.8) {
                    batt = ((volts - 74.8) / 3.0) * 0.2 + 0.6;
                } else if (volts > 71.8) {
                    batt = ((volts - 71.8) / 3.0) * 0.2 + 0.4;
                } else if (volts > 70.3) {
                    batt = ((volts - 70.3) / 1.5) * 0.2 + 0.2;
                } else if (volts > 68.0) {
                    batt = ((volts - 68.0) / 2.3) * 0.2;
                } else {
                    batt = 0.0;
                }
            }
        }
        return (int)(batt * 100.0);
    }

    private static String getLegacyWorkModeString(int value) {
        switch (value & 0xF) {
            case 0:
                return "Idle";
            case 1:
                return "Drive";
            case 2:
                return "Zero";
            case 3:
                return "LargeAngle";
            case 4:
                return "Check";
            case 5:
                return "Lock";
            case 6:
                return "Error";
            case 7:
                return "Carry";
            case 8:
                return "RemoteControl";
            case 9:
                return "Shutdown";
            case 10:
                return "pomStop";
            case 12:
                return "Unlock";
            default:
                return "Unknown";
        }
    }

    private static String getWorkModeString(int value) {
        int hValue = value >> 4;
        String result;
        switch (hValue) {
            case 1:
                result = "Shutdown";
                break;
            case 2:
                result = "Drive";
                break;
            case 3:
                result = "Charging";
                break;
            default:
                result = "Unknown code " + hValue;
                break;
        }
        if ((value & 0xF) == 1) {
            result += " - Engine off";
        }
        return result;
    }

    public static String getModelString(Model model) {
        switch (model.getValue()) {
            case "0":
                return "Inmotion R1N";
            case "1":
                return "Inmotion R1S";
            case "2":
                return "Inmotion R1CF";
            case "3":
                return "Inmotion R1AP";
            case "4":
                return "Inmotion R1EX";
            case "5":
                return "Inmotion R1Sample";
            case "6":
                return "Inmotion R1T";
            case "7":
                return "Inmotion R10";
            case "10":
                return "Inmotion V3";
            case "11":
                return "Inmotion V3C";
            case "12":
                return "Inmotion V3PRO";
            case "13":
                return "Inmotion V3S";
            case "21":
                return "Inmotion R2N";
            case "22":
                return "Inmotion R2S";
            case "23":
                return "Inmotion R2Sample";
            case "20":
                return "Inmotion R2";
            case "24":
                return "Inmotion R2EX";
            case "30":
                return "Inmotion R0";
            case "60":
                return "Inmotion L6";
            case "61":
                return "Inmotion Lively";
            case "50":
                return "Inmotion V5";
            case "51":
                return "Inmotion V5PLUS";
            case "52":
                return "Inmotion V5F";
            case "53":
                return "Inmotion V5D";
            case "80":
                return "Inmotion V8";
            case "85":
                return "Solowheel Glide 3";
            case "86":
                return "Inmotion V8F";
            case "87":
                return "Inmotion V8S";
            case "100":
                return "Inmotion V10S";
            case "101":
                return "Inmotion V10SF";
            case "140":
                return "Inmotion V10";
            case "141":
                return "Inmotion V10F";
            case "142":
                return "Inmotion V10T";
            case "143":
                return "Inmotion V10FT";
            default:
                return "Unknown";
        }
    }



    public static class CANMessage {
        enum CanFormat {
            StandardFormat(0),
            ExtendedFormat(1);

            private final int value;

            CanFormat(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum CanFrame {
            DataFrame(0),
            RemoteFrame(1);

            private final int value;

            CanFrame(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum IDValue {
            NoOp(0),
            GetFastInfo(0x0F550113),
            GetSlowInfo(0x0F550114),
            RideMode(0x0F550115),
            RemoteControl(0x0F550116),
            Calibration(0x0F550119),
            PinCode(0x0F550307),
            Light(0x0F55010D),
            HandleButton(0x0F55012E),
            SpeakerVolume(0x0F55060A),
            PlaySound(0x0F550609),
            Alert(0x0F780101);

            private final int value;

            IDValue(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int id = IDValue.NoOp.getValue();
        byte[] data = new byte[8];
        int len = 0;
        int ch = 0;
        int format = CanFormat.StandardFormat.getValue();
        int type = CanFrame.DataFrame.getValue();
        byte[] ex_data;

        CANMessage(byte[] bArr) {
            if (bArr.length < 16) return;
            id = (((bArr[3] * 256) + bArr[2]) * 256 + bArr[1]) * 256 + bArr[0];
            data = Arrays.copyOfRange(bArr, 4, 12);
            len = bArr[12];
            ch = bArr[13];
            format = bArr[14] == 0 ? CanFormat.StandardFormat.getValue() : CanFormat.ExtendedFormat.getValue();
            type = bArr[15] == 0 ? CanFrame.DataFrame.getValue() : CanFrame.RemoteFrame.getValue();

            if (len == (byte) 0xFE) {
                int ldata = MathsUtil.intFromBytesLE(data, 0);

                if (ldata == bArr.length - 16) {
                    ex_data = Arrays.copyOfRange(bArr, 16, 16 + ldata);
                }
            }

        }

        public boolean isValid() {
            return ex_data != null;
        }

        private CANMessage() {
        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();
            byte check = computeCheck(canBuffer);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0xAA);
            out.write(0xAA);

            try {
                out.write(escape(canBuffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.write(check);
            out.write(0x55);
            out.write(0x55);

            return out.toByteArray();
        }

        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();

            int b3 = id / (256 * 256 * 256);
            int b2 = (id - b3 * 256 * 256 * 256) / (256 * 256);

            int b1 = (id - b3 * 256 * 256 * 256 - b2 * 256 * 256) / 256;
            int b0 = id % 256;

            buff.write(b0);
            buff.write(b1);
            buff.write(b2);
            buff.write(b3);

            try {
                buff.write(data);
                buff.write(len);
                buff.write(ch);
            } catch (IOException e) {
                e.printStackTrace();
            }

            buff.write(format == CanFormat.StandardFormat.getValue() ? 0 : 1);
            buff.write(type == CanFrame.DataFrame.getValue() ? 0 : 1);

            if (len == (byte) 0xFE) {
                try {
                    buff.write(ex_data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return buff.toByteArray();
        }

        public void clearData() {
            data = new byte[data.length];
        }

        private static byte computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = (check + c) & 0xFF;
            }
            return (byte) check;
        }

        static CANMessage verify(byte[] buffer) {

            if (buffer[0] != (byte) 0xAA || buffer[1] != (byte) 0xAA || buffer[buffer.length - 1] != (byte) 0x55 || buffer[buffer.length - 2] != (byte) 0x55) {
                return null;  // Header and tail not correct
            }
            Timber.i("Before escape %s", StringUtil.toHexString(buffer));
            int len = buffer.length - 3;
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, len);

            Timber.i("After escape %s", StringUtil.toHexString(dataBuffer));
            byte check = CANMessage.computeCheck(dataBuffer);

            byte bufferCheck = buffer[len];
            if (check == bufferCheck) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, calc: %02X, packet: %02X", check, bufferCheck);
            }
            return (check == bufferCheck) ? new CANMessage(dataBuffer) : null;

        }

        private byte[] escape(byte[] buffer) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte c : buffer) {
                if (c == (byte) 0xAA || c == (byte) 0x55 || c == (byte) 0xA5) {
                    out.write(0xA5);
                }
                out.write(c);
            }
            return out.toByteArray();
        }

        public static CANMessage standardMessage() {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.GetFastInfo.getValue();
            msg.ch = 5;
            msg.data = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1};
            return msg;
        }

        public static CANMessage getFastData() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetFastInfo.getValue();
            msg.ch = 5;
            msg.data = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

            return msg;
        }

        public static CANMessage getSlowData() {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.GetSlowInfo.getValue();
            msg.ch = 5;
            msg.type = CanFrame.RemoteFrame.getValue();
            msg.data = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            return msg;
        }

        public static CANMessage setLight(boolean on) {
            CANMessage msg = new CANMessage();
            byte enable = 0;
            if (on) {
                enable = 1;
            }
            msg.len = 8;
            msg.id = IDValue.Light.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{enable, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage setLed(boolean on) {
            CANMessage msg = new CANMessage();
            byte enable = 0x10;
            if (on) {
                enable = 0x0F;
            }
            msg.len = 8;
            msg.id = IDValue.RemoteControl.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0xB2, (byte) 0x00, (byte) 0x00, (byte) 0x00, enable, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage wheelBeep() {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.RemoteControl.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0xB2, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage wheelCalibration() {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.Calibration.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0x32, (byte) 0x54, (byte) 0x76, (byte) 0x98, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage powerOff() {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.RemoteControl.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0xB2, 0, 0, 0, 5, 0, 0, 0};
            return msg;
        }

        public static CANMessage setHandleButton(boolean on) {
            CANMessage msg = new CANMessage();
            byte enable = 1;
            if (on) {
                enable = 0;
            }
            msg.len = 8;
            msg.id = IDValue.HandleButton.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{enable, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage setMaxSpeed(int maxSpeed) {
            CANMessage msg = new CANMessage();
            byte[] value = MathsUtil.getBytes((short)(maxSpeed * 1000));
            msg.len = 8;
            msg.id = IDValue.RideMode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{1, 0, 0, 0, value[1], value[0], 0, 0};

            return msg;
        }

        public static CANMessage playSound(byte soundNumber) {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.PlaySound.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{soundNumber, 0, 0, 0, 0, 0, 0, 0};

            return msg;
        }

        public static CANMessage setRideMode(boolean rideMode) {
            /// rideMode =0 -Comfort, =1 -Classic
            byte classic = 0;
            if (rideMode) {
                classic = 1;
            }
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.RideMode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0x0a, (byte) 0x00, (byte) 0x00, (byte) 0x00, classic, (byte) 0x00 , (byte) 0x00, (byte) 0x00};

            return msg;
        }

        public static CANMessage setPedalSensivity(int sensivity) {
            byte[] value = MathsUtil.getBytes((short)((sensivity+28)<<5));
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.RideMode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, value[1], value[0] , (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage setSpeakerVolume(int speakerVolume) {
            CANMessage msg = new CANMessage();
            int lowByte = (speakerVolume * 100) & 0xFF;
            int highByte = ((speakerVolume * 100) / 0x100) & 0xFF;
            msg.len = 8;
            msg.id = IDValue.SpeakerVolume.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) lowByte, (byte) highByte, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            return msg;
        }

        public static CANMessage setTiltHorizon(int tiltHorizon) {
            CANMessage msg = new CANMessage();
            int tilt = tiltHorizon * 65536 / 10;

            byte[] t = MathsUtil.getBytes(tilt);
            msg.len = 8;
            msg.id = IDValue.RideMode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{0, 0, 0, 0, t[3], t[2], t[1], t[0]};

            return msg;
        }


        public static CANMessage getBatteryLevelsdata() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetSlowInfo.getValue();
            msg.ch = 5;
            msg.type = CanFrame.RemoteFrame.getValue();
            msg.data = new byte[]{0, 0, 0, 15, 0, 0, 0, 0};

            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.GetSlowInfo.getValue();
            msg.ch = 5;
            msg.type = CanFrame.RemoteFrame.getValue();
            msg.data = new byte[]{32, 0, 0, 0, 0, 0, 0, 0};
            return msg;
        }

        public static CANMessage getPassword(String password) {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.PinCode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            byte[] pass = password.getBytes();
            msg.data = new byte[]{pass[0], pass[1], pass[2], pass[3], pass[4], pass[5], 0, 0};
            return msg;
        }

        public static CANMessage setMode(int mode) {
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.NoOp.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0xB2, 0, 0, 0, (byte) mode, 0, 0, 0};
            return msg;
        }

        boolean parseFastInfoMessage(Model model) {
            if (!isValid()) return false;
            double angle = (double) (MathsUtil.intFromBytesLE(ex_data, 0)) / 65536.0;
            double roll = (double) (MathsUtil.intFromBytesLE(ex_data, 72)) / 90.0;
            double speed = ((double) (MathsUtil.intFromBytesLE(ex_data, 12)) + (double) (MathsUtil.intFromBytesLE(ex_data, 16))) / (model.getSpeedCalculationFactor() * 2.0);
            speed = Math.abs(speed);
            int voltage = MathsUtil.intFromBytesLE(ex_data, 24);
            int current = (int)MathsUtil.signedIntFromBytesLE(ex_data, 20);
            int temperature = ex_data[32];
            int temperature2 = ex_data[34];
            int batt = batteryFromVoltage(voltage, model);
            long totalDistance;
            long distance;
            if (model.belongToInputType("1") || model.belongToInputType("5") ||
                    model == V8 || model == Glide3 || model == V10 || model == V10F ||
                    model == V10S || model == V10SF || model == V10T || model == V10FT ||
                    model == V8F || model == V8S) {
                totalDistance = (MathsUtil.intFromBytesLE(ex_data, 44)); ///// V10F 48 byte - trip distance
            } else if (model == R0) {
                totalDistance = (MathsUtil.longFromBytesLE(ex_data, 44));

            } else if (model == L6) {
                totalDistance = (MathsUtil.longFromBytesLE(ex_data, 44)) * 100;

            } else {
                totalDistance = Math.round((MathsUtil.longFromBytesLE(ex_data, 44)) / 5.711016379455429E7d);
            }
            distance = (MathsUtil.intFromBytesLE(ex_data, 48));

            String workMode;
            int workModeInt = MathsUtil.intFromBytesLE(ex_data, 60);
            if (model == V8F || model == V8S || model == V10 || model == V10F || model == V10FT ||
                    model == V10S || model == V10SF || model == V10T) {
                roll = 0;
                workMode = getWorkModeString(workModeInt);
            } else {
                workMode = getLegacyWorkModeString(workModeInt);
            }

            WheelData wd = WheelData.getInstance();
            wd.setAngle(angle);
            wd.setRoll(roll);
            wd.setSpeed((int)(speed * 360d));
            wd.setVoltage(voltage);
            wd.setBatteryLevel(batt);
            wd.setCurrent(current);
            wd.setTotalDistance(totalDistance);
            wd.setWheelDistance(distance);
            wd.setTemperature(temperature*100);
            wd.setTemperature2(temperature2*100);
            wd.setModeStr(workMode);
            wd.calculatePwm();
            wd.calculatePower();

            return true;
        }

        boolean parseAlertInfoMessage() {
            int alertId = data[0];
            double alertValue = (data[3] * 256) | (data[2] & 0xFF);
            double alertValue2 = (data[7] * 256 * 256 * 256) | ((data[6] & 0xFF) * 256 * 256) | ((data[5] & 0xFF) * 256) | (data[4] & 0xFF);
            double a_speed = Math.abs((alertValue2 / 3812.0) * 3.6);
            String fullText;

            StringBuilder hex = new StringBuilder("[");
            for (int c : data) {
                hex.append(String.format("%02X", (c & 0xFF)));
            }
            hex.append("]");
            switch (alertId) {
                case 0x05:
                    fullText = String.format(Locale.ENGLISH, "Start from tilt angle %.2f at speed %.2f %s", (alertValue / 100.0), a_speed, hex.toString());
                    break;
                case 0x06:
                    fullText = String.format(Locale.ENGLISH, "Tiltback at speed %.2f at limit %.2f %s", a_speed, (alertValue / 1000.0), hex.toString());
                    break;
                case 0x19:
                    fullText = String.format(Locale.ENGLISH, "Fall Down %s", hex.toString());
                    break;
                case 0x20:
                    fullText = String.format(Locale.ENGLISH, "Low battery at voltage %.2f %s", (alertValue2 / 100.0), hex.toString());
                    break;
                case 0x21:
                    fullText = String.format(Locale.ENGLISH, "Speed cut-off at speed %.2f and something %.2f %s", a_speed, (alertValue / 10.0), hex.toString());
                    break;
                case 0x26:
                    fullText = String.format(Locale.ENGLISH, "High load at speed %.2f and current %.2f %s", a_speed, (alertValue / 1000.0), hex.toString());
                    break;
                case 0x1d:
                    fullText = String.format(Locale.ENGLISH, "Please repair: bad battery cell found. At voltage %.2f %s", (alertValue2 / 100.0), hex.toString());
                    break;
                default:
                    fullText = String.format(Locale.ENGLISH, "Unknown Alert %.2f %.2f, please contact palachzzz, hex %s", alertValue, alertValue2, hex.toString());
            }
            WheelData wd = WheelData.getInstance();
            wd.setAlert(fullText);
            return true;
        }


        boolean parseSlowInfoMessage() {
            if (!isValid()) return false;
            Model lmodel = Model.findByBytes(ex_data);  // CarType is just model.rawValue
            if (lmodel == UNKNOWN) lmodel = V8;
            int v0 = ex_data[27] & 0xFF;
            int v1 = ex_data[26] & 0xFF;
            int v2 = ((ex_data[25] & 0xFF) * 256) | (ex_data[24] & 0xFF);
            String version = String.format(Locale.ENGLISH, "%d.%d.%d", v0, v1, v2);
            StringBuilder serialNumber = new StringBuilder();
            int maxspeed;
            int speakervolume = 0;
            boolean light = ex_data[80] == 1;
            boolean led = false;
            boolean handlebutton = false;
            boolean rideMode = false;
            int pedalHardness = 100;
            int pedals = (int) (Math.round((MathsUtil.intFromBytesLE(ex_data, 56)) / 6553.6));
            AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            maxspeed = (((ex_data[61] & 0xFF) * 256) | (ex_data[60] & 0xFF)) / 1000;
            if (ex_data.length > 126) {
                speakervolume = (((ex_data[126] & 0xFF) * 256) | (ex_data[125] & 0xFF)) / 100;
            }
            if (ex_data.length > 130) {
                led = ex_data[130] == 1;
            }
            if (ex_data.length > 129) {
                handlebutton = ex_data[129] != 1;
            }
            if (ex_data.length > 132) {
                rideMode = ex_data[132] == 1;
            }
            if (ex_data.length > 124) {
                pedalHardness = (ex_data[124]-28) & 0xFF; // 0x80 = 128 = 100% -maximum, 0x20 = 32 - minimum
            }

            for (int j = 0; j < 8; j++) {
                serialNumber.append(String.format("%02X", ex_data[7 - j]));
            }

            WheelData wd = WheelData.getInstance();
            wd.setSerial(serialNumber.toString());
            wd.setModel(getModelString(lmodel));
            wd.setVersion(version);

            appConfig.setLightEnabled(light);
            appConfig.setLedEnabled(led);
            appConfig.setHandleButtonDisabled(handlebutton);
            appConfig.setWheelMaxSpeed(maxspeed);
            appConfig.setSpeakerVolume(speakervolume);
            appConfig.setPedalsAdjustment(pedals);
            appConfig.setRideMode(rideMode);
            appConfig.setPedalSensivity(pedalHardness);
            getInstance().setModel(lmodel);
            return false;
        }

        public byte[] getData() {
            return data;
        }
    }

    static class InMotionUnpacker {
        enum UnpackerState {
            unknown,
            collecting,
            done
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        // there are two types of packets, basic and extended, if it is extended packet,
        // then len field should be 0xFE, and len of extended data should be in first data byte
        // of usual packet
        int len_p = 0;  // basic packet len
        int len_ex = 0; // extended packet len

        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {
            if (c != (byte) 0xA5 || oldc == (byte) 0xA5) {
                if (state == UnpackerState.collecting) {
                    buffer.write(c);
                    int sz = buffer.size();
                    if (sz == 7) len_ex = c & 0xFF;
                    else if (sz == 15) len_p = c & 0xFF;
                    if ((sz > len_ex+21) && (len_p == 0xFE)) {
                        reset(); // longer than expected
                        return false;
                    }
                    if ((c == (byte) 0x55 && oldc == (byte) 0x55) && ((sz == len_ex+21) || (len_p != 0xFE))) { // 18 header + 1 crc + 2 footer
                        state = UnpackerState.done;
                        updateStep = 0;
                        oldc = 0;
                        Timber.i("Step reset");
                        return true;
                    }
                } else {
                    if (c == (byte) 0xAA && oldc == (byte) 0xAA) {
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0xAA);
                        buffer.write(0xAA);
                        state = UnpackerState.collecting;
                    }
                }

            }
            oldc = c;
            return false;
        }

        void reset(){
            buffer = new ByteArrayOutputStream();
            oldc = 0;
            len_p = 0;
            len_ex = 0;
            state = UnpackerState.unknown;
        }
    }

    @Override
    public int getCellsForWheel() {
        return 20;
    }

    public static synchronized InMotionAdapter getInstance() {
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new InMotionAdapter();
        } else {
            Timber.i("Get instance");
        }
        return INSTANCE;
    }

    public static synchronized void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("New instance");
        INSTANCE = new InMotionAdapter();
    }

    public static synchronized void stopTimer() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("Kill instance, stop timer");
        INSTANCE = null;
    }
}
