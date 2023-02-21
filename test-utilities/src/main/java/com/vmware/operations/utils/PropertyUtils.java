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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests often have a need for customization based on properties, which can be
 * defined as environment variables or JVM properties.
 * <p>
 * This class defines some low-level routines for loading and using properties.
 */
public class PropertyUtils {
    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);

    /**
     * Private constructor for static utils class.
     */
    private PropertyUtils() {
    }

    /**
     * Get the name of the local user.  This is used to disambiguate and attribute test products
     * back to developers.
     *
     * The local user can be configured two ways:
     *
     * It looks first at the system environment variable "TEST_USERNAME".
     *
     * If that is not set, then it looks at the built-in Java property "user.name". This
     * property returns the current user account name on most operating systems.
     *
     * If neither is found, it will assert.
     *
     * @return a string containing a reference that indicates the local user
     */
    public static String getLocalUserName() {
        String result = System.getenv().get("TEST_USERNAME");
        if (result == null) {
            result = System.getProperty("user.name");
        }
        if (result == null) {
            throw new AssertionError("Local user name not found.  Please set the environment variable 'TEST_USERNAME'");
        }
        return result;
    }

    /**
     * Get a shortened name of the local user.  This is used to disambiguate and attribute test products
     * back to developers.
     *
     * The local user short name can be configured two ways:
     *
     * It looks first at the system environment variable "TEST_SHORT_USERNAME".
     *
     * If that is not set, then it calls getLocalUserName() and uses the first two
     * characters.
     *
     * @return a short string containing a reference that indicates the local user
     */
    public static String getLocalUserShortName() {
        String result = tryGetProperty("TEST_SHORT_USERNAME");
        if (result == null) {
            result = getLocalUserName().substring(0, 2);
        }
        return result;
    }

    /**
     * Using the local user short name, build a random string string with a reasonable
     * amount of entropy such that we wouldn't expect the name to be returned twice if we ask
     * for 1 million names. (&lt; 0.001% chance)
     *
     * @return unique string that can be used to generate test objects, but is attributed to a user.
     */
    public static String getRandomizedName() {
        return FuzzUtils.getRandomizedString(getLocalUserShortName() + "-", FuzzUtils.UTF16_CHARS, 9, 12);
    }

    /**
     * Using the local user short name, build a random string string with a reasonable
     * amount of entropy that we wouldn't expect the name to be returned twice if we ask
     * for 1 million names. (&lt; 0.001% chance)
     *
     * @return unique string that can be used to generate test objects, but is attributed to a user.
     */
    public static String getRandomizedUrlName() {
        return FuzzUtils.getRandomizedString(getLocalUserShortName() + "-", FuzzUtils.URL_CHARS, 9, 12);
    }

    /**
     * Utility method to encrypt strings (in a reversible way), for tests.
     *
     * This allows passwords to be checked into a repository, and decoded based
     * on the shared secret in an environment variable (TEST_ENCRYPTION_KEY).
     *
     * @param plaintextString UTF-8 string to encode
     * @return Encrypted version of the string (using UTF8 character set).
     * @throws Exception if there is a problem while encrypting
     */
    public static String encryptString(String plaintextString) throws Exception {
        final Charset utf8 = Charset.forName("UTF-8");
        String cipherKey = getProperty("TEST_ENCRYPTION_KEY");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cipherKey.getBytes(utf8), "AES"));

        byte[] plaintextBytes = plaintextString.getBytes(utf8);
        byte[] encryptedBytes = cipher.doFinal(plaintextBytes);
        return new String(Base64.encodeBase64(encryptedBytes), utf8);
    }

    /**
     * Utility method to decrypt strings encrypted using the method above.
     *
     * This allows passwords to be checked into a repository, and decoded based
     * on the shared secret in an environment variable (TEST_ENCRYPTION_KEY).
     *
     * @param encryptedString UTF-8 encoded encryption string
     * @return unencrypted string
     * @throws AssertionError if the encryption key is incorrect.
     */
    public static String decryptString(String encryptedString) {
        final Charset utf8 = Charset.forName("UTF-8");
        String cipherKey = getProperty("TEST_ENCRYPTION_KEY");

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cipherKey.getBytes(utf8), "AES"));

            byte[] encryptedBytes = Base64.decodeBase64(encryptedString);
            byte[] plaintextBytes = cipher.doFinal(encryptedBytes);
            return new String(plaintextBytes, utf8);
        } catch (Exception ex) {
            throw new AssertionError("Failed to decrypt string: " + encryptedString, ex);
        }
    }

    /**
     * Get a value from the environment that can be used as a guide for how many repetitions
     * to perform when doing stress tests.
     *
     * The actual stress numbers should be scaled based on the specific test.  As a general rule,
     * stress tests should take no longer than 1 minute when run with the default
     * stress factor of 10.
     *
     * The default can be changed through the system environment variable "TEST_STRESS_FACTOR".
     *
     * @return an integer representing the desired stress factor
     */
    public static int getStressScaleFactor() {
        String env = tryGetProperty("TEST_STRESS_FACTOR");
        if (env != null) {
            return Integer.parseInt(env);
        }
        return 10;
    }

    /**
     * Compare a value from the environment with a value written into the test code.
     * If the user has declared that the system-under-test is older than that specified
     * version, then this method will return true.
     *
     * If no version is defined in the environment (the default), or is defined incorrectly,
     * then this method will return false.
     *
     * @param productVersion current version of the component
     * @param testVersion version to compare against
     * @return negative if productVersion is less than testVersion, positive if
     * product version is greater than testVersion.
     */
    public static int compareVersions(String productVersion, String testVersion) {
        String[] prodParts = productVersion.split("\\.");
        String[] testParts = testVersion.split("\\.");
        int length = Math.max(prodParts.length, testParts.length);
        for (int i = 0; i < length; i++) {
            int prodPart = i < prodParts.length ? Integer.parseInt(prodParts[i]) : 0;
            int testPart = i < testParts.length ? Integer.parseInt(testParts[i]) : 0;
            if (prodPart != testPart) {
                return testPart - prodPart;
            }
        }

        return 0;
    }

    /**
     * Load a property from the system environment, and if it doesn't exist, then load it from a
     * properties file.
     *
     * @param propertyKey key for the property
     * @return the value of the property, or null if the property can't be found.
     */
    public static String tryGetProperty(String propertyKey) {
        String result = System.getenv(propertyKey);
        if (result == null) {
            result = System.getProperty(propertyKey);
        }
        return result;
    }

    /**
     * Load a property from the system environment, and if it doesn't exist, then load it from a
     * properties file.
     *
     * @param propertyKey key for the property
     * @return the value of the property, or throws IllegalStateException if the property can't be found.
     */
    public static String getProperty(String propertyKey) {
        String result = tryGetProperty(propertyKey);
        if (result == null) {
            throw new IllegalStateException("Couldn't find property '" + propertyKey + "'.  Define a system environment variable, or include the appropriate properties file.");
        }
        return result;
    }

    /**
     * Load a property from the system environment, and if it doesn't exist, then load it from a
     * properties file.
     *
     * @param propertyKey key for the property
     * @param defaultValue value to return if property is not defined.
     * @return the value of the property, or throws IllegalStateException if the property can't be found.
     */
    public static String getPropertyOrDefault(String propertyKey, String defaultValue) {
        String result = tryGetProperty(propertyKey);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    /**
     * Indexed properties allow for a master variable to select among a suite of alternatives.
     * <p>
     * Create a full property key from the component bits.  The result will semantically
     * be "prefix.${indexor}.propertyKey".
     *
     * @param indexor     key for the indexing variable
     * @param prefix      prefix for the property key
     * @param propertyKey key for the property
     * @return the expanded key for the indexed property.
     */
    public static String expandIndexedProperty(String indexor, String prefix, String propertyKey) {
        String index = getProperty(indexor);
        return prefix + "." + index + "." + propertyKey;
    }

    /**
     * Indexed properties allow for a master variable to select among a suite of alternatives.
     * Using the specified arguments, a three-token property key (separated by dots) is created,
     * then the final value is looked up as an environment variable or system property.
     * <p>
     * Load a property value from the environment variables, and if it doesn't exist, then load it from a
     * Java system properties.
     *
     * @param indexor key for the property whose value is used as the second token in the final property key
     * @param prefix first token of the property key
     * @param propertyKey third token for the property key
     * @return the value of the property, or null if the property can't be found.
     */
    public static String tryGetIndexedProperty(String indexor, String prefix, String propertyKey) {
        return tryGetProperty(expandIndexedProperty(indexor, prefix, propertyKey));
    }

    /**
     * Indexed properties allow for a master variable to select among a suite of alternatives.
     * Using the specified arguments, a three-token property key (separated by dots) is created,
     * then the final value is looked up as an environment variable or system property.
     * <p>
     * Load a property value from the environment variables, and if it doesn't exist, then load it from a
     * Java system properties.
     *
     * @param indexor key for the property whose value is used as the second token in the final property key
     * @param prefix first token of the property key
     * @param propertyKey third token for the property key
     * @return the value of the property, or throws IllegalStateException if the property can't be found.
     */
    public static String getIndexedProperty(String indexor, String prefix, String propertyKey) {
        return getProperty(expandIndexedProperty(indexor, prefix, propertyKey));
    }

    /**
     * Indexed properties allow for a master variable to select among a suite of alternatives.
     * Using the specified arguments, a three-token property key (separated by dots) is created,
     * then the final value is looked up as an environment variable or system property.
     * <p>
     * Load a property value from the environment variables, and if it doesn't exist, then load it from a
     * Java system properties.
     *
     * @param indexor key for the property whose value is used as the second token in the final property key
     * @param prefix first token of the property key
     * @param propertyKey third token for the property key
     * @param defaultValue value to return if property is not defined.
     * @return the value of the property, or throws IllegalStateException if the property can't be found.
     */
    public static String getIndexedPropertyOrDefault(String indexor, String prefix, String propertyKey, String defaultValue) {
        String result = tryGetIndexedProperty(indexor, prefix, propertyKey);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    /**
     * Expand instances of ${VAR} in a string, and return the result.
     * Bindings for variables can come from the specified map and/or the
     * environment (including Java system properties).
     * <p>
     * If there is no variable matching VAR (case-sensitive), then the entire ${VAR} reference will be removed.
     *
     * @param text a string, optionally containing ${VAR} substrings.
     * @param bindings a map of variable bindings to use, or null
     * @param useEnvironment use the environment variable and Java system properties if variables cannot be
     *                       found in the bindings map.
     * @return expanded string
     */
    public static String expandProperties(String text, Map<String, String> bindings, boolean useEnvironment) {
        final String pattern = "\\$\\{([A-Za-z0-9]+)\\}";
        Pattern expr = Pattern.compile(pattern);
        Matcher matcher = expr.matcher(text);
        while (matcher.find()) {
            String var = matcher.group(1);
            String value = null;
            if (bindings != null) {
                value = bindings.get(var);
            }
            if (value == null & useEnvironment) {
                value = tryGetProperty(var);
            }
            if (value == null) {
                value = "";
            } else {
                value = value.replace("\\", "\\\\");
            }
            Pattern subexpr = Pattern.compile(Pattern.quote(matcher.group(0)));
            text = subexpr.matcher(text).replaceAll(value);
        }
        return text;
    }
}
