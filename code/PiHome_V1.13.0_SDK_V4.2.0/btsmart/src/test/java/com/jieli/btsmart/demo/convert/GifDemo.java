package com.jieli.btsmart.demo.convert;

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
class GifDemo {

    /**
     * gif转换
     * <p>
     *   旧接口<br/>
     *   默认是JL701N芯片，JL UI格式
     *</p>
     */
    public void gif2Bin() {
        //获取Gif转码对象
        GifConverter converter = GifConverter.getInstance();
        //inputPath : 输入文件路径 (GIF文件)
        String inputPath = "输入文件路径(GIF文件)";
        //outputPath : 输出文件路径 (BIN文件)
        String outputPath = "输出文件路径(BIN文件)";
        //mode: 编码模式
        int mode = GifConverter.MODE_LOW_COMPRESSION_RATE; //建议是低压缩率
        // - GifConverter.MODE_LOW_COMPRESSION_RATE: 低压缩率
        // - GifConverter.MODE_MEDIUM_COMPRESSION_RATE：中压缩率
        // - GifConverter.MODE_HIGH_COMPRESSION_RATE ： 高压缩率
        //执行GIF转码功能
        converter.gif2BinAsync(inputPath, outputPath, mode, new GifConverter.ResultCallback<GifBin>() {
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

    public void gif2BinNew() {
        //获取Gif转码对象
        GifConverter converter = GifConverter.getInstance();
        //inputPath : 输入文件路径 (GIF文件)
        String inputPath = "输入文件路径(GIF文件)";
        //outputPath : 输出文件路径 (BIN文件)
        String outputPath = "输出文件路径(BIN文件)";
        //mode: 编码模式
        int mode = GifConverter.MODE_LOW_COMPRESSION_RATE; //建议是低压缩率
        // - GifConverter.MODE_LOW_COMPRESSION_RATE: 低压缩率
        // - GifConverter.MODE_MEDIUM_COMPRESSION_RATE：中压缩率
        // - GifConverter.MODE_HIGH_COMPRESSION_RATE ： 高压缩率
        //V1.2.0+ 增加, 默认值: GifConverter.CHIP_701N
        int chip = GifConverter.CHIP_707N; //根据固件芯片来选择算法
        // - GifConverter.CHIP_701N : JL701N系列芯片
        // - GifConverter.CHIP_707N : JL707N系列芯片
        //V1.3.0+ 增加，默认值: true
        boolean isPacketJLUI = true; //是否打包成JL UI格式; false，为裸数据
        //执行GIF转码功能
        converter.gif2BinAsync(inputPath, outputPath, mode, chip, isPacketJLUI, new GifConverter.ResultCallback<GifBin>() {
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
