package com.jieli.btsmart;

import com.jieli.jl_rcsp.util.CHexConver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testData() {
        int[] array = new int[]{254, 220, 186, 192, 12, 0, 15, 3, 0, 15, 0, 1, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 239};
        byte[] data = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            data[i] = CHexConver.intToByte(array[i]);
        }
        System.out.print("data = " + CHexConver.byte2HexStr(data));
    }
}