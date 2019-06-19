/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ike.eap.message.attributes;

import static com.android.ike.eap.message.EapSimAttribute.EAP_AT_PADDING;
import static com.android.ike.eap.message.attributes.EapTestAttributeDefinitions.AT_PADDING;
import static com.android.ike.eap.message.attributes.EapTestAttributeDefinitions.AT_PADDING_INVALID_PADDING;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.ike.eap.exceptions.EapSimInvalidAtPaddingException;
import com.android.ike.eap.message.EapSimAttribute;
import com.android.ike.eap.message.EapSimAttribute.AtPadding;
import com.android.ike.eap.message.EapSimAttributeFactory;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

public class AtPaddingTest {
    private static final int EXPECTED_LENGTH = 8;

    private EapSimAttributeFactory mEapSimAttributeFactory;

    @Before
    public void setUp() {
        mEapSimAttributeFactory = EapSimAttributeFactory.getInstance();
    }

    @Test
    public void testDecode() throws Exception {
        ByteBuffer input = ByteBuffer.wrap(AT_PADDING);
        EapSimAttribute result = mEapSimAttributeFactory.getEapSimAttribute(input);

        assertFalse(input.hasRemaining());
        assertTrue(result instanceof AtPadding);
        AtPadding atPadding = (AtPadding) result;
        assertEquals(EAP_AT_PADDING, atPadding.attributeType);
        assertEquals(EXPECTED_LENGTH, atPadding.lengthInBytes);
    }

    @Test
    public void testDecodeInvalidPadding() throws Exception {
        ByteBuffer input = ByteBuffer.wrap(AT_PADDING_INVALID_PADDING);
        try {
            mEapSimAttributeFactory.getEapSimAttribute(input);
            fail("Expected EapSimInvalidAtPaddingException for nonzero padding bytes");
        } catch (EapSimInvalidAtPaddingException expected) {
        }
    }

    @Test
    public void testEncode() throws Exception {
        AtPadding atPadding = new AtPadding(EXPECTED_LENGTH);

        ByteBuffer result = ByteBuffer.allocate(EXPECTED_LENGTH);
        atPadding.encode(result);

        assertFalse(result.hasRemaining());
        assertArrayEquals(AT_PADDING, result.array());
    }
}
