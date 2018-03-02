package com.vmware.utils;

import org.junit.Assert;
import org.testng.annotations.Test;

public class FuzzUtilsTest {
    @Test
    public void adaptEmptyString() {
        String nullString = null;
        FuzzUtils.adaptString(nullString, FuzzUtils.UTF8_CHARS, FuzzUtils.BASIC_CHARS);

        String emptyString = "";
        FuzzUtils.adaptString(emptyString, FuzzUtils.UTF8_CHARS, FuzzUtils.BASIC_CHARS);
    }

    @Test
    public void adaptTest() {
        String utf16 = FuzzUtils.getRandomizedString("", FuzzUtils.UTF16_CHARS, 0, 20);

        String utf8 = FuzzUtils.adaptString(utf16, FuzzUtils.UTF16_CHARS, FuzzUtils.UTF8_CHARS);
        Assert.assertEquals(utf16.length(), utf8.length());
        Assert.assertNotEquals("UTF8 should be different than UTF16", utf16, utf8);

        String utf8_utf16 = FuzzUtils.adaptString(utf16, FuzzUtils.UTF8_CHARS, FuzzUtils.UTF16_CHARS);
        Assert.assertEquals("Round trip through UTF8 should return the original:" + utf8, utf16, utf8_utf16);

        String url = FuzzUtils.adaptString(utf16, FuzzUtils.UTF16_CHARS, FuzzUtils.URL_CHARS);
        Assert.assertEquals(utf16.length(), utf8.length());
        Assert.assertNotEquals("URL should be different than UTF16", utf16, url);
        Assert.assertNotEquals("URL should be different than UTF8", utf8, url);

        String url_utf8 = FuzzUtils.adaptString(url, FuzzUtils.URL_CHARS, FuzzUtils.UTF8_CHARS);
        Assert.assertEquals("Round trip through URL should return the original UTF8:" + url, utf8, url_utf8);

        String url_utf16 = FuzzUtils.adaptString(url, FuzzUtils.URL_CHARS, FuzzUtils.UTF16_CHARS);
        Assert.assertEquals("Round trip through URL should return the original UTF16:" + url, utf16, url_utf16);

        String basic = FuzzUtils.adaptString(utf16, FuzzUtils.UTF16_CHARS, FuzzUtils.BASIC_CHARS);
        Assert.assertEquals(utf16.length(), basic.length());
        Assert.assertNotEquals("Basic should be different than UTF16", utf16, basic);

        String utf8_basic = FuzzUtils.adaptString(utf8, FuzzUtils.UTF8_CHARS, FuzzUtils.BASIC_CHARS);
        Assert.assertEquals("Conversions to basic from UTF8 should be the same", basic, utf8_basic);

        String url_basic = FuzzUtils.adaptString(url, FuzzUtils.URL_CHARS, FuzzUtils.BASIC_CHARS);
        Assert.assertEquals("Conversions to basic from URL should be the same", basic, url_basic);
    }

    @Test
    public void randomizerEmptyString() {
        String nullString = FuzzUtils.getRandomizedString(null, FuzzUtils.UTF8_CHARS, 0, 10);
        Assert.assertEquals(10, nullString.length());

        String emptyString = FuzzUtils.getRandomizedString("", FuzzUtils.UTF8_CHARS, 0, 10);
        Assert.assertEquals(10, emptyString.length());
    }


    @Test
    public void randomizerTest() {
        String zero = FuzzUtils.getRandomizedString(null, FuzzUtils.UTF8_CHARS, 0, 0);
        Assert.assertEquals(0, zero.length());

        String entropy = FuzzUtils.getRandomizedString(null, FuzzUtils.UTF8_CHARS, 5, 0);
        Assert.assertEquals(5, entropy.length());

        String length = FuzzUtils.getRandomizedString(null, FuzzUtils.UTF8_CHARS, 0, 5);
        Assert.assertEquals(5, length.length());

        String moreEntropy = FuzzUtils.getRandomizedString(null, FuzzUtils.UTF8_CHARS, 8, 5);
        Assert.assertEquals(8, moreEntropy.length());

        String lessEntropy = FuzzUtils.getRandomizedString(null, FuzzUtils.UTF8_CHARS, 3, 5);
        Assert.assertEquals(5, lessEntropy.length());
    }

    @Test
    public void randomizerPrefixTest() {
        String prefix = "12characters";

        String zero = FuzzUtils.getRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 0, 0);
        Assert.assertEquals(12, zero.length());

        String entropy = FuzzUtils.getRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 5, 0);
        Assert.assertEquals(17, entropy.length());

        String length = FuzzUtils.getRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 0, 5);
        Assert.assertEquals(12, length.length());

        String moreEntropy = FuzzUtils.getRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 8, 15);
        Assert.assertEquals(20, moreEntropy.length());

        String lessEntropy = FuzzUtils.getRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 3, 15);
        Assert.assertEquals(15, lessEntropy.length());
    }

    @Test
    public void timeStringTest() throws InterruptedException {
        String nullString = FuzzUtils.getTimeRandomizedString(null, FuzzUtils.UTF8_CHARS, 0, 3);
        Assert.assertEquals(3, nullString.length());

        String emptyString = FuzzUtils.getTimeRandomizedString("", FuzzUtils.UTF8_CHARS, 0, 3);
        Assert.assertEquals(3, emptyString.length());

        String prefix = "12characters";

        String zero = FuzzUtils.getTimeRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 0, 0);
        Assert.assertEquals(15, zero.length());

        String entropy = FuzzUtils.getTimeRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 5, 0);
        Assert.assertEquals(17, entropy.length());

        String length = FuzzUtils.getTimeRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 0, 5);
        Assert.assertEquals(15, length.length());

        String moreEntropy = FuzzUtils.getTimeRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 8, 15);
        Assert.assertEquals(20, moreEntropy.length());

        String lessEntropy = FuzzUtils.getTimeRandomizedString(prefix, FuzzUtils.UTF8_CHARS, 3, 15);
        Assert.assertEquals(15, lessEntropy.length());
    }

    @Test
    public void timeStringCompareTest() throws InterruptedException {
        String older = FuzzUtils.getTimeRandomizedString(null, FuzzUtils.UTF8_CHARS, 0, 3);
        Assert.assertEquals(3, older.length());

        Thread.sleep(100);

        String newer = FuzzUtils.getTimeRandomizedString(null, FuzzUtils.UTF8_CHARS, 0, 3);
        Assert.assertEquals(3, newer.length());
        Assert.assertTrue("newer should always be lexicographically less", newer.compareTo(older) <= 0);
    }
}
