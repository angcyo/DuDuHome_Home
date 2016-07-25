package com.dudu.android.launcher;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testTPMS() {
        byte[] bytes = {0x62, 0x00, 0x28, 0x1e, 0x3c, 0x14, 0x20, 0x12, 0x20, 0x12, 0x20, 0x12, 0x46, 0x72};
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        float fH = byteBuffer.get(2) / 10f;//第一轴 高压
        float fL = byteBuffer.get(3) / 10f;//第一轴 低压

        float bH = byteBuffer.get(4) / 10f;//第二轴 高压
        float bL = byteBuffer.get(5) / 10f;//第二轴 高压

        int tempH = byteBuffer.get(12) - 50;//高温
        assertFalse("message", false);
    }
}