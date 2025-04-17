package com.jieli.btsmart.demo.convert;

import com.jieli.bmp_convert.BmpConvert;
import com.jieli.bmp_convert.OnConvertListener;

/**
 * BmpDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 * @since 2024/7/5
 */
class BmpDemo {

    /**
     * bitmap图像转换
     */
    public void convertPhoto() {
        //1.初始化图片转换对象
        BmpConvert bmpConvert = new BmpConvert();
        String inPath = "输入文件路径"; //图像格式， png, jpg
        String outPath = "输出文件路径"; //输出文件格式。二进制文件
        //2.开始图像转换
        //用户需要根据设备的芯片和性能来选择算法
        int flag = BmpConvert.TYPE_701N_RGB; //JL701N-WATCH-SDK 图像转换算法 - RGB
        //        flag = BmpConvert.TYPE_695N_RBG;   //AC695N-WATCH-SDK 图像转换算法 - RGB
        //        flag = BmpConvert.TYPE_701N_ARGB; //JL701N-WATCH-SDK 图像转换算法 - ARGB
        //        flag = BmpConvert.TYPE_701N_RGB_NO_PACK;   //JL701N-WATCH-SDK 图像转换算法 - RGB & 不打包封装
        //        flag = BmpConvert.TYPE_701N_ARGB_NO_PACK;  //JL701N-WATCH-SDK 图像转换算法 - ARGB & 不打包封装
        //        flag = BmpConvert.TYPE_707N_RGB;  //AC707N-WATCH-SDK 图像转换算法 - RGB
        //        flag = BmpConvert.TYPE_707N_ARGB; //AC707N-WATCH-SDK 图像转换算法 - ARGB
        //        flag = BmpConvert.TYPE_707N_RGB_NO_PACK;  //AC707N-WATCH-SDK 图像转换算法 - RGB & 不打包封装
        //        flag = BmpConvert.TYPE_707N_ARGB_NO_PACK; //AC707N-WATCH-SDK 图像转换算法 - ARGB & 不打包封装
        bmpConvert.bitmapConvert(flag, inPath, outPath, new OnConvertListener() {

            @Override
            public void onStart(String path) {
                //回调转换开始
                //path: 输入文件路径
            }

            @Override
            public void onStop(boolean result, String output) {
                //回调转换结束
                //result： 转换结果
                //output： 输出文件路径
            }
        });
        //3.不需要使用图片转换功能时，需要释放图片转换对象
        //        bmpConvert.release();
    }
}
