package com.jieli.btsmart.demo.convert;

import com.jieli.bmp_convert.BmpConvert;
import com.jieli.bmp_convert.ConvertParam;
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
     */
    public void convertPhoto() {
        //1.初始化图片转换对象
        BmpConvert bmpConvert = new BmpConvert();
        String inPath = "输入文件路径"; //图像格式， png, jpg
        String outPath = "输出文件路径"; //输出文件格式。二进制文件
        //2.开始图像转换
        //用户需要根据设备的芯片和性能来选择算法
        int flag = BmpConvert.TYPE_701N_RGB;                 //701N图像转换算法 - RGB
        //        flag = BmpConvert.TYPE_695N_RBG;           //695N图像转换算法 - RGB
        //V1.1.0+ 支持
        //        flag = BmpConvert.TYPE_701N_ARGB;          //701N图像转换算法 - ARGB
        //V1.2.0+ 支持
        //        flag = BmpConvert.TYPE_701N_RGB_NO_PACK;   //701N图像转换算法 - RGB & 不打包封装
        //        flag = BmpConvert.TYPE_701N_ARGB_NO_PACK;  //701N图像转换算法 - ARGB & 不打包封装
        //V1.4.0+ 支持
        //        flag = BmpConvert.TYPE_707N_RGB;           //707N图像转换算法 - RGB
        //        flag = BmpConvert.TYPE_707N_ARGB;          //707N图像转换算法 - ARGB
        //        flag = BmpConvert.TYPE_707N_RGB_NO_PACK;   //707N图像转换算法 - RGB & 不打包封装
        //        flag = BmpConvert.TYPE_707N_ARGB_NO_PACK;  //707N图像转换算法 - ARGB & 不打包封装
        //V1.6.0+支持
        //        flag = BmpConvert.TYPE_701N_JPEG;          //701N图像转换算法 - JPEG
        //V1.6.0+ 支持
        //指定输出编码格式， 默认值: ConvertParam.FORMAT_ARGB_8565
        int format = ConvertParam.FORMAT_AUTO;              //自动选取压缩后最小的格式，ARGB8565或者ARGB8888
        //        format = ConvertParam.FORMAT_ARGB_8565;   //指定输出ARGB8565格式。 若算法是RGB算法，会变成指定输出RGB565格式
        //        format = ConvertParam.FORMAT_ARGB_8888;   //指定输出ARGB8888格式。 若算法是RGB算法，会变成指定输出RGB888格式
        //构造转换参数
        ConvertParam param = new ConvertParam().setFormat(format);
        //执行图像转码功能
        bmpConvert.bitmapConvert(flag, inPath, outPath, param, new OnConvertListener() {

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

            @Override
            public void onStop(ConvertResult convertResult, String s) {

            }
        });
        //3.不需要使用图片转换功能时，需要释放图片转换对象
        //        bmpConvert.release();
    }
}
