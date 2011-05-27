/*
 * Copyright 2004-2008 H2 Group. Licensed under the H2 License, Version 1.0
 * (license2)
 * Initial Developer: H2 Group
 */
package org.h2.test.unit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.h2.compress.LZFInputStream;
import org.h2.compress.LZFOutputStream;
import org.h2.test.TestBase;

/**
 * Tests the LZF stream.
 */
public class TestStreams extends TestBase {

    public void test() throws Exception {
        testLZFStreams();
    }

    byte[] getRandomBytes(Random random) {
        int[] sizes = new int[] { 0, 1, random.nextInt(1000), random.nextInt(100000), random.nextInt(1000000) };
        int size = sizes[random.nextInt(sizes.length)];
        byte[] buffer = new byte[size];
        if (random.nextInt(5) == 1) {
            random.nextBytes(buffer);
        } else if (random.nextBoolean()) {
            int patternLen = random.nextInt(100) + 1;
            for (int j = 0; j < size; j++) {
                buffer[j] = (byte) (j % patternLen);
            }
        }
        return buffer;
    }

    private void testLZFStreams() throws Exception {
        Random random = new Random(1);
        int max = getSize(100, 1000);
        for (int i = 0; i < max; i += 3) {
            byte[] buffer = getRandomBytes(random);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            LZFOutputStream comp = new LZFOutputStream(out);
            if (random.nextInt(10) == 1) {
                comp.write(buffer);
            } else {
                for (int j = 0; j < buffer.length;) {
                    int[] sizes = new int[] { 0, 1, random.nextInt(100), random.nextInt(100000) };
                    int size = sizes[random.nextInt(sizes.length)];
                    size = Math.min(size, buffer.length - j);
                    if (size == 1) {
                        comp.write(buffer[j]);
                    } else {
                        comp.write(buffer, j, size);
                    }
                    j += size;
                }
            }
            comp.close();
            byte[] compressed = out.toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(compressed);
            LZFInputStream decompress = new LZFInputStream(in);
            byte[] test = new byte[buffer.length];
            for (int j = 0; j < buffer.length;) {
                int[] sizes = new int[] { 0, 1, random.nextInt(100), random.nextInt(100000) };
                int size = sizes[random.nextInt(sizes.length)];
                if (size == 1) {
                    int x = decompress.read();
                    if (x < 0) {
                        break;
                    }
                    test[j++] = (byte) x;
                } else {
                    size = Math.min(size, test.length - j);
                    int l = decompress.read(test, j, size);
                    if (l < 0) {
                        break;
                    } else {
                        j += l;
                    }
                }
            }
            check(buffer, test);
        }
    }

}