package com.jieli.btsmart.demo;

import com.jieli.lib.gif.GifConverter;
import com.jieli.lib.gif.model.GifBin;

/**
 * GifDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Gif编码示例代码
 * @since 2024/6/21
 */
public class GifDemo {


    public void gif2Bin(String inputPath, String outputPath) {
        GifConverter converter = GifConverter.getInstance();
        //inputPath : 输入文件路径 (GIF文件)
        //outputPath : 输出文件路径 (BIN文件)
        //mode: 编码模式
        // - GifConverter.MODE_LOW_COMPRESSION_RATE: 低压缩率
        // - GifConverter.MODE_MEDIUM_COMPRESSION_RATE：中压缩率
        // - GifConverter.MODE_HIGH_COMPRESSION_RATE ： 高压缩率
        converter.gif2BinAsync(inputPath, outputPath, GifConverter.MODE_LOW_COMPRESSION_RATE, new GifConverter.ResultCallback<GifBin>() {
            @Override
            public void onSuccess(GifBin gifBin) {
                //转码成功
                //gifBin --- GIF信息
            }

            @Override
            public void onError(int code, String message) {
                //转码失败
                //code --- 错误码
                //message --- 描述信息
            }
        });
    }
}
