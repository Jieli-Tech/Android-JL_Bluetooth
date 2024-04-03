package com.jieli.btsmart;

import com.jieli.bluetooth.bean.base.BasePacket;
import com.jieli.bluetooth.bean.command.file_op.DeleteFileByNameCmd;
import com.jieli.bluetooth.bean.command.file_op.StartLargeFileTransferCmd;
import com.jieli.bluetooth.bean.parameter.StartLargeFileTransferParam;
import com.jieli.bluetooth.tool.ParseHelper;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.jl_fatfs.utils.FatUtil;
import com.jieli.jl_rcsp.model.command.file_op.LargeFileTransferGetNameCmd;
import com.jieli.jl_rcsp.util.JL_Log;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/9/9 2:30 PM
 * @desc :
 */
public class BTSmartTest {

    @Test
    public void testRgbAndHslConvert() {
        byte[] data = {0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde, 0x0f};
        long result = CHexConver.bytesToLong(data, 1, 2);
        System.out.println("result-->" + result);

        System.out.println(FatUtil.getFatFsName("bg_watch.bin"));
        System.out.println(FatUtil.getFatFsName("bg_watch123.bin"));
    }

    @Test
    public void testLargeFileTransferCmd() {
        JL_Log.setUseTest(true);
        Date date = new Date();


        String hashStr = String.format("%08x", Long.valueOf(1234).hashCode());
        JL_Log.e("sen", "hashStr result = " + hashStr);


        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(new Date().getTime());
        String timeStr1 = String.format("%d%02d%02d%02d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));//14

        String test = "test" + 123 + new Date().getTime();

        try {
            byte[] result = MessageDigest.getInstance("md5").digest(test.getBytes());
            JL_Log.e("sen", "result = " + CHexConver.byte2HexStr(result));
            try {
                JL_Log.e("sen", "result = " + CHexConver.byte2HexStr(result).getBytes("unicode").length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            JL_Log.e("sen", "result = " + new String(result));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        JL_Log.e("sen", "timeStr1 = " + timeStr1 + "\t" + UUID.randomUUID().toString());


        date.setYear(2300);
        String timeStr = String.format("0000000000000000%s", Long.toHexString(date.getTime()));
        String timeHash = timeStr.substring(timeStr.length() - 16);

        JL_Log.d("sen", "==========current=======" + timeHash + "\t" + Long.toHexString("大发送到发送大发送到发的算法是对方到发送到发送到f".hashCode()) + "\t" + "大发送到发送大发送到发的算法是对方到发送到发送到f".hashCode());
        JL_Log.d("sen", "==========current=======" + CHexConver.byte2HexStr(CHexConver.hexStr2Bytes(timeHash)));


        byte[] hash = new byte[16];
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) i;
        }
        int size = 0x0102;
        StartLargeFileTransferParam param = new StartLargeFileTransferParam(hash, size, (short) 0);
        StartLargeFileTransferCmd cmd = new StartLargeFileTransferCmd(param);

        BasePacket basePacket = ParseHelper.convert2BasePacket(cmd, BasePacket.TYPE_COMMAND);
        JL_Log.d("sen", basePacket.toString());
        byte data[] = ParseHelper.packSendBasePacket(basePacket);
        JL_Log.d("sen", CHexConver.byte2HexStr(data));


        JL_Log.d("sen", "==========StopLargeFileTransferCmd=======");

//        StopLargeFileTransferParam param1 = new StopLargeFileTransferParam();
//        param1.setName("123");
//
//
//        StopLargeFileTransferCmd stopLargeFileTransferCmd = new StopLargeFileTransferCmd(param1);
//        StopLargeFileTransferResponse response = new StopLargeFileTransferResponse();
//        response.setName("123");
//        stopLargeFileTransferCmd.setResponse(response);
//        stopLargeFileTransferCmd.setStatus(StateCode.STATUS_SUCCESS);
//
//
//        basePacket = ParseHelper.convert2BasePacket(stopLargeFileTransferCmd, BasePacket.TYPE_RESPONSE);
//        JL_Log.d("sen", basePacket.toString());
//        data = ParseHelper.packSendBasePacket(basePacket);
//        JL_Log.d("sen", CHexConver.byte2HexStr(data));

        JL_Log.setUseTest(false);
    }

    @Test
    public void testFileName() {

        LargeFileTransferGetNameCmd.Param param = new LargeFileTransferGetNameCmd.Param("test.mp3", 0);
//       System.out.println(CHexConver.byte2HexStr(param.getParamData()));

//        param = new LargeFileTransferGetNameCmd.Param("test.mp3", 1);
//        System.out.println(CHexConver.byte2HexStr(param.getParamData()));
//
//        param = new LargeFileTransferGetNameCmd.Param("test.mp3", 12);
//        System.out.println(CHexConver.byte2HexStr(param.getParamData()));

        param = new LargeFileTransferGetNameCmd.Param("test奥迪地方.mp3", 0);
        System.out.println(CHexConver.byte2HexStr(param.getParamData()) + "\tsize = " + param.getParamData().length);

        param = new LargeFileTransferGetNameCmd.Param("test奥迪地方.mp3", 1);
        System.out.println(CHexConver.byte2HexStr(param.getParamData()) + "\tsize = " + param.getParamData().length);


//        param = new LargeFileTransferGetNameCmd.Param("test奥迪地方.mp3", 12);
//        System.out.println(CHexConver.byte2HexStr(param.getParamData()));
//
//        param = new LargeFileTransferGetNameCmd.Param("发送到发送到佛即可啦就收到反馈是大房间里卡是江东父老看见阿里斯顿积分卡上的.mp3", 189);
//        System.out.println(CHexConver.byte2HexStr(param.getParamData()));
//
//
//        param = new LargeFileTransferGetNameCmd.Param("a发送到发送到佛即可啦就收到反馈是大房间里卡是江东父老看见阿里斯顿积分卡上的.mp3", 189);
//        System.out.println(CHexConver.byte2HexStr(param.getParamData()));


        String str = "fa\u0012df df\u0020a\u0032d\u0019";


        try {
            System.out.println(str + "\t" + str.length() + "\t" + removeIllegalChar(str) + "\t" + removeIllegalChar(str).length());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }

    private String removeIllegalChar(String input) throws UnsupportedEncodingException {
        byte[] data = input.getBytes("utf-16BE");
        System.out.println(CHexConver.byte2HexStr(data));
        byte[] resultData = new byte[data.length];
        int i = 0;
        int index = 0;
        for (i = 0; i < data.length; i += 2) {
            int code = ((data[i] << 8) & 0xff) + (0xff & data[i + 1]);
            System.out.println("code --" + code + "\t i = " + i);
            if (code > 0x20) {
                resultData[index++] = data[i];
                resultData[index++] = data[i + 1];
            }
        }

        return new String(resultData, 0, index, "utf-16BE");

    }


    @Test
    public void testDeleteFileByNameCmd() {
        JL_Log.setUseTest(true);
        DeleteFileByNameCmd deleteFileByNameCmd = new DeleteFileByNameCmd(new DeleteFileByNameCmd.Param("count"));
        BasePacket basePacket = ParseHelper.convert2BasePacket(deleteFileByNameCmd, BasePacket.TYPE_COMMAND);
        JL_Log.d("sen", basePacket.toString());
        byte data[] = ParseHelper.packSendBasePacket(basePacket);
        JL_Log.d("sen", CHexConver.byte2HexStr(data));

    }


    @Test
    public void testByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.put(new byte[]{0x01, 0x02, 0x03});
        System.out.println("byteBuffer position = " + byteBuffer.position() + "\tcapacity = " + byteBuffer.capacity());
        System.out.println(" byteBuffer.array len " + byteBuffer.array().length);
    }


   /* @Test
    public void testCmdSn() {
        Thread thread = null;
        for (int i = 0; i < 10; i++) {
            thread = new Thread(new PrintSn(), "Thread_" + i);
            thread.start();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class PrintSn implements Runnable {
        private final SimpleDateFormat yyyyMMdd_HHmmssSSS = new SimpleDateFormat("yyyyMMddHHmmss.SSS", Locale.getDefault());

        @Override
        public void run() {
            String time = yyyyMMdd_HHmmssSSS.format(Calendar.getInstance().getTime());
            String name = Thread.currentThread().getName();
            System.out.println(String.format("Thread-name : %s start, time=%s", name, time));
            int sn = BluetoothUtil.autoIncSN();
            time = yyyyMMdd_HHmmssSSS.format(Calendar.getInstance().getTime());
            System.out.println(String.format("Thread-name : %s, sn = %d, time=%s", name, sn, time));
        }
    }*/
}
