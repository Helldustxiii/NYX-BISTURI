package com.nyx.bridge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImageFormatTest {

    @Test
    public void detectsJpegSignature() {
        byte[] header = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        assertEquals(ImageFormat.Kind.JPEG, ImageFormat.detect(header, header.length));
    }

    @Test
    public void detectsPngSignature() {
        byte[] header = {
                (byte)  0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
        assertEquals(ImageFormat.Kind.PNG, ImageFormat.detect(header, header.length));
    }

    @Test
    public void detectsWebpContainer() {
        byte[] header = {
                'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P'
        };
        assertEquals(ImageFormat.Kind.WEBP, ImageFormat.detect(header, header.length));
    }

    @Test
    public void rejectsUnknownAndTruncatedSignatures() {
        assertNull(ImageFormat.detect(new byte[] {'G', 'I', 'F', '8', '9', 'a'}, 6));
        assertNull(ImageFormat.detect(new byte[] {(byte) 0xFF, (byte) 0xD8}, 2));
        assertNull(ImageFormat.detect(null, 0));
    }
}
