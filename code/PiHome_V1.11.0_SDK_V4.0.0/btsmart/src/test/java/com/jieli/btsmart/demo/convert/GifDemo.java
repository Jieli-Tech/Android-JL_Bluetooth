package com.jieli.btsmart.demo.convert;

import com.jieli.lib.gif.GifConverter;
import com.jieli.lib.gif.model.GifBin;

import org.junit.Test;

/**
 * GifDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Gif编码示例代码
 * @since 2024/6/21
 */
class GifDemo {

    /**
     * gif转换
     * @param inputPath 输入文件路径<p>例如：in.gif</p>
     * @param outputPath  输出文件路径<p>例如:out.res, out</p>
     */
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
