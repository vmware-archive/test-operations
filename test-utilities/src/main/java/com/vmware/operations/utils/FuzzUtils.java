/*
 * Copyright (c) 2017-2018 VMware, Inc. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vmware.operations.utils;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

/**
 * Utilities to manipulate string values used by various functional tests.
 * <p>
 * This class introduces a concept of character sets, which are sets of character that are valid in particular
 * situations.  Character sets also hve the ability to be "adapted" between characters sets.
 * <p>
 * All character sets have equal length (76 characters), so that the adaptation will work between
 * any two character sets.  See comments in {@link #adaptString} for limitations.
 */
public class FuzzUtils {

    /**
     * Private constructor for static utils class.
     */
    private FuzzUtils() {
    }

    /**
     * A character set used for the most constrictive requirements. Contains only numbers and
     * uppercase characters.  The set is repetitive, which means adapting strings to this set will work,
     * but adapting strings from this set will not get the full range of the result set
     */
    public static final String BASIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZABC0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZXYZ";

    /**
     * A character set used for (case-sensitive) url requirements. Contains only characters valid in both parameters
     * and path segments (See RFC 3986 - pchar and sub-delim). No repetition.
     */
    public static final String URL_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_-.)!@+$';&*(abcdefghijklmnopqrstuvwxyz=~,";

    /**
     * A character set consisting of single-byte UTF8 characters including punctuation. No repetition.
     */
    public static final String UTF8_CHARS = "0123456789ÄßÇDÈFGHÎJK£MÑØ¶Q®§†Ü√WX¥Z_-.)!@#$%^&*(åbç∂éƒghîjk|µñøπ'\"stü\\w{}<>[]";

    /**
     * A character set consisting of value UTF16 characters from many unicode code pages.
     * No repetition, includes punctuation and whitespace characters
     */
    public static final String UTF16_CHARS = "\u24e1\u2460\u24f6\u2675\u2643\u2464\u277b\u2786\u2791\u0669\u00c5\u00df\u00c7\u00d0\u20ac\u1e1e\u20b2\u021e\u0399\u0134\ua7a2\u00a3\u039c\u00d1\u00d8\u2119\u213a\u13d2\u2c7e\u01ac\u22c3\u2207\u0428\u00d7\u00a5\u017d_-." +
            ")!@#$%^&*(\u00c5\u00df\u00c7\u00d0\u20ac\u1e1e\u20b2\u10ac\u0399\u0134\ua7a2|\u039c\u00d1\u00d8\u2119'\"\u2c7e\u01ac\u22c3\\ {}<>[]";

    /**
     * Using a prefix, build a random string string with a given length, filled
     * with random chars from the specified charset.  The first few characters will
     * be time based (roughly an hour granularity), and when sorted the newer
     * strings will be lexicographically less (i.e. they are sorted z-a)
     *
     * @param prefix fixed prefix string that will be at the beginning of the result.
     * @param charset 78 character string that will be used to generate the random parts of the result.
     * @param minEntropy The minimum number of random characters that will be added to the prefix
     * @param minLength The minimum length of the result.  Note that if the prefix length + minEntropy + 3 is
     *                  greater than minLength, the resulting string will be longer than minLength.
     * @return unique string that can be used to generate test objects.
     */
    public static String getTimeRandomizedString(String prefix, String charset, int minEntropy, int minLength) {

        if (prefix == null) {
            prefix = "";
        }

        String timeEntropy = getTimeEntropyPrefix(3_600_000, 3);

        minEntropy -= timeEntropy.length();
        if (minEntropy < 0) {
            minEntropy = 0;
        }

        return getRandomizedString(prefix + timeEntropy, charset, minEntropy, minLength);
    }

    /**
     * Calculate a prefix based on the current time.
     * first part of the entropy string is time based, with later times appearing lexicographically earlier,
     * so that recent objects will be first in the paged lists.
     * @param granularity number of milliseconds between different values
     * @param numChars number of characters to return
     */
    private static String getTimeEntropyPrefix(int granularity, int numChars) {
        long millisSinceEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();

        // We do this by calculating a time delta until the end of time, then finding hours and modding to come
        // up with a decreasing, cyclic, time-based prefix.
        long intervalsUntilEndOfTime = (-millisSinceEpoch & 0x7FFF_FFFF_FFFF_FFFFL) / granularity;
        int cyclicIntervals = (int) (intervalsUntilEndOfTime % (36 ^ numChars));
        String timeEntropy = Integer.toString(cyclicIntervals, 36);

        while (timeEntropy.length() < numChars) {
            timeEntropy = Integer.toString(0) + timeEntropy;
        }

        return timeEntropy;
    }

    /**
     * Using a prefix, build a random string string with a given length, filled
     * with random chars from the specified charset.
     *
     * @param prefix fixed prefix string that will be at the beginning of the result.
     * @param charset 78 character string that will be used to generate the random parts of the result.
     * @param minEntropy The minimum number of random characters that will be added to the prefix
     * @param minLength The minimum length of the result.  Note that if the prefix length + minEntropy is
     *                  greater than minLength, the resulting string will be longer than minLength.
     * @return unique string that can be used to generate test objects.
     */
    public static String getRandomizedString(String prefix, String charset, int minEntropy, int minLength) {
        StringBuilder result = new StringBuilder(minLength);

        int entropy = minLength;

        if (prefix != null) {
            result.append(prefix);
            entropy -= prefix.length();
        }

        if (entropy < minEntropy) {
            entropy = minEntropy;
        }

        // Add the minimum number of random digits
        Random r = new Random();
        for (int i = 0; i < entropy; ++i) {
            result.append(charset.charAt(r.nextInt(charset.length())));
        }

        return result.toString();
    }

    /**
     * Utility to convert strings from one charset (as defined ny this class) to another.
     *
     * Not all character sets are fully unique, so when adapting A &rarr; B &rarr; A', A != A' unless
     * the B character set is unique across the domain of the A character set.
     *
     * Example:
     * String s = getRandomizedString("test", UTF16_CHARS, 0, 10) = testĴß0⋃⓶∇Ð
     * String t = adaptString(s, UTF16_CHARS, URL_CHARS)          = testJB0U2VB
     *
     * @param s             the string to adapt
     * @param sourceCharset the original character set
     * @param destCharset   the new character set to use
     * @return new string with characters from dest character set, or null if s was null
     */
    public static String adaptString(String s, String sourceCharset, String destCharset) {
        if (s == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            int index = sourceCharset.indexOf(c);
            if (index >= 0 && index < destCharset.length()) {
                result.append(destCharset.charAt(index));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
