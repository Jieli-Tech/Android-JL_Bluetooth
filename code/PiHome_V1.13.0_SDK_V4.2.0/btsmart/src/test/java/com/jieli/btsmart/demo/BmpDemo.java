package com.jieli.btsmart.demo;

import com.jieli.bmp_convert.BmpConvert;
import com.jieli.bmp_convert.ConvertResult;
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
     *
     * @param inPath  输入文件路径<p>例如：in.png ,in.jpg</p>
     * @param outPath 输出文件路径<p>例如:out.res, out</p>
     */
    public void convertPhoto(String inPath, String outPath) {
        //1.初始化图片转换对象
        BmpConvert bmpConvert = new BmpConvert();
        //判断算法类型
        int flag = BmpConvert.TYPE_BR_28; //JL701N-WATCH-SDK 图像转换算法 - RGB
        //flag = BmpConvert.TYPE_BR_23;   //AC695N-WATCH-SDK 图像转换算法 - RGB
        //flag = BmpConvert.TYPE_BR_28_ALPHA; //JL701N-WATCH-SDK 图像转换算法 - ARGB
        //flag = BmpConvert.TYPE_BR_28_RAW;   //JL701N-WATCH-SDK 图像转换算法 - RGB & 不打包封装
        //flag = BmpConvert.TYPE_BR_28_ALPHA_RAW;  //JL701N-WATCH-SDK 图像转换算法 - ARGB & 不打包封装
        //2.开始图像转换
        bmpConvert.bitmapConvert(flag, inPath, outPath, new OnConvertListener() {
            //回调转换开始
            //path: 输入文件路径
            @Override
            public void onStart(String path) {

            }

            //回调转换结束
            //result： 转换结果
            //output： 输出文件路径
            @Override
            public void onStop(boolean result, String output) {

            }

            @Override
            public void onStop(ConvertResult convertResult, String s) {

            }
        });
        //3.不需要使用图片转换功能时，需要释放图片转换对象
        //bmpConvert.release();
    }
}
