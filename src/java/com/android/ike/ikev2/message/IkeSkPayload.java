/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.ike.ikev2.message;

import com.android.ike.ikev2.exceptions.IkeException;
import com.android.ike.ikev2.message.IkePayload.PayloadType;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * IkeSkPayload represents a Encrypted Payload.
 *
 * <p>It contains other payloads in encrypted form. It is must be the last payload in the message.
 * It should be the only payload in this implementation.
 *
 * <p>Critical bit must be ignored when doing decoding.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#page-105">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2).
 */
public final class IkeSkPayload extends IkePayload {

    public final byte[] unencryptedPayloads;

    /**
     * Construct an instance of IkeSkPayload in the context of {@link IkePayloadFactory}.
     *
     * @param critical indicates if it is a critical payload.
     * @param message the byte array contains the whole IKE message.
     * @param integrityMac the initialized Mac for integrity check.
     * @param expectedChecksumLen the expected length of integrity checksum.
     * @param decryptCipher the uninitialized Cipher for doing decryption.
     * @param dKey the decryption key.
     * @param expectedIvLen the expected length of Initialization Vector.
     */
    IkeSkPayload(
            boolean critical,
            byte[] message,
            Mac integrityMac,
            int expectedChecksumLen,
            Cipher decryptCipher,
            SecretKey dKey,
            int expectedIvLen)
            throws IkeException, GeneralSecurityException {
        super(PAYLOAD_TYPE_SK, critical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(message);

        // Skip IKE header and SK payload header
        byte[] tempArray = new byte[IkeHeader.IKE_HEADER_LENGTH + GENERIC_HEADER_LENGTH];
        inputBuffer.get(tempArray);

        // Extract bytes for authentication and decryption.
        byte[] iv = new byte[expectedIvLen];

        int encryptedDataLen =
                message.length
                        - (IkeHeader.IKE_HEADER_LENGTH
                                + GENERIC_HEADER_LENGTH
                                + expectedIvLen
                                + expectedChecksumLen);
        // IkeMessage will catch exception if encryptedDataLen is negative.
        byte[] encryptedData = new byte[encryptedDataLen];

        byte[] integrityChecksum = new byte[expectedChecksumLen];
        inputBuffer.get(iv).get(encryptedData).get(integrityChecksum);

        // Authenticate and decrypt.
        validateChecksumOrThrow(message, integrityMac, expectedChecksumLen, integrityChecksum);
        unencryptedPayloads = decrypt(encryptedData, decryptCipher, dKey, iv);
    }

    // TODO: Add another constructor for AEAD protected payload.

    private void validateChecksumOrThrow(
            byte[] message, Mac integrityMac, int expectedChecksumLen, byte[] integrityChecksum)
            throws GeneralSecurityException {
        ByteBuffer inputBuffer = ByteBuffer.wrap(message, 0, message.length - expectedChecksumLen);
        integrityMac.update(inputBuffer);
        byte[] calculatedChecksum =
                Arrays.copyOfRange(integrityMac.doFinal(), 0, expectedChecksumLen);

        if (!Arrays.equals(integrityChecksum, calculatedChecksum)) {
            throw new GeneralSecurityException("Message authentication failed. ");
        }
    }

    private byte[] decrypt(byte[] encryptedData, Cipher decryptCipher, SecretKey dKey, byte[] iv)
            throws GeneralSecurityException {
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        decryptCipher.init(Cipher.DECRYPT_MODE, dKey, ivParameterSpec);

        ByteBuffer inputBuffer = ByteBuffer.wrap(encryptedData);
        ByteBuffer outputBuffer = ByteBuffer.allocate(encryptedData.length);
        decryptCipher.doFinal(inputBuffer, outputBuffer);

        // Remove padding
        outputBuffer.rewind();
        int padLength = Byte.toUnsignedInt(outputBuffer.get(encryptedData.length - 1));
        byte[] decryptedData = new byte[encryptedData.length - padLength - 1];

        outputBuffer.get(decryptedData);
        return decryptedData;
    }

    /**
     * Throw an Exception when trying to encode this payload.
     *
     * @throws UnsupportedOperationException for this payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        // TODO: Implement thie method
        throw new UnsupportedOperationException(
                "It is not supported to encode a " + getTypeString());
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        // TODO: Implement thie method
        throw new UnsupportedOperationException(
                "It is not supported to get length of  a " + getTypeString());
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "Encrypted and Authenticated Payload";
    }
}