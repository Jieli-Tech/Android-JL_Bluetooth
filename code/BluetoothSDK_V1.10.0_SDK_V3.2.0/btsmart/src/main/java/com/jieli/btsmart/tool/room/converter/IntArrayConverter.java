package com.jieli.btsmart.tool.room.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * @ClassName: IntegerConverter
 * @Description:
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/6/27 17:16
 */
public class IntArrayConverter {
    @TypeConverter
    public String objectToString(float[] array) {
        return new Gson().toJson(array);
    }

    @TypeConverter
    public float[] stringToObject(String json) {
        Type listType = new TypeToken<float[]>() {
        }.getType();
        return new Gson().fromJson(json, listType);
    }

    @TypeConverter
    public String objectToString(int[] array) {
        return new Gson().toJson(array);
    }

    @TypeConverter
    public int[] stringToObject1(String json) {
        Type listType = new TypeToken<int[]>() {
        }.getType();
        return new Gson().fromJson(json, listType);
    }
}
