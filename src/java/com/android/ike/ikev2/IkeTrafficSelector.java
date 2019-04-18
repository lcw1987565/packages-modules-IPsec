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

package com.android.ike.ikev2;

import android.annotation.IntDef;
import android.util.ArraySet;

import com.android.ike.ikev2.exceptions.InvalidSyntaxException;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * IkeTrafficSelector represents a Traffic Selector of a Child SA.
 *
 * <p>IkeTrafficSelector can be constructed by users for initiating Create Child exchange or be
 * constructed from a decoded inbound Traffic Selector Payload.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.13">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeTrafficSelector {

    // IpProtocolId consists of standard IP Protocol IDs.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IP_PROTOCOL_ID_UNSPEC, IP_PROTOCOL_ID_ICMP, IP_PROTOCOL_ID_TCP, IP_PROTOCOL_ID_UDP})
    public @interface IpProtocolId {}

    // Zero value is re-defined by IKE to indicate that all IP protocols are acceptable.
    @VisibleForTesting static final int IP_PROTOCOL_ID_UNSPEC = 0;
    @VisibleForTesting static final int IP_PROTOCOL_ID_ICMP = 1;
    @VisibleForTesting static final int IP_PROTOCOL_ID_TCP = 6;
    @VisibleForTesting static final int IP_PROTOCOL_ID_UDP = 17;

    private static final ArraySet<Integer> IP_PROTOCOL_ID_SET = new ArraySet<>();

    static {
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_UNSPEC);
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_ICMP);
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_TCP);
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_UDP);
    }

    /**
     * TrafficSelectorType consists of IKE standard Traffic Selector Types.
     *
     * @see <a
     *     href="https://www.iana.org/assignments/ikev2-parameters/ikev2-parameters.xhtml">Internet
     *     Key Exchange Version 2 (IKEv2) Parameters</a>
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE, TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE})
    public @interface TrafficSelectorType {}

    public static final int TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE = 7;
    public static final int TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE = 8;

    public static final int PORT_NUMBER_MIN = 0;
    public static final int PORT_NUMBER_MAX = 65535;

    // TODO: Consider defining these constants in a central place in Connectivity.
    private static final int IPV4_ADDR_LEN = 4;
    private static final int IPV6_ADDR_LEN = 16;

    @VisibleForTesting static final int TRAFFIC_SELECTOR_IPV4_LEN = 16;
    @VisibleForTesting static final int TRAFFIC_SELECTOR_IPV6_LEN = 40;

    public final int tsType;
    public final int ipProtocolId;
    public final int selectorLength;
    public final int startPort;
    public final int endPort;
    public final InetAddress startingAddress;
    public final InetAddress endingAddress;

    private IkeTrafficSelector(
            int tsType,
            int ipProtocolId,
            int selectorLength,
            int startPort,
            int endPort,
            InetAddress startingAddress,
            InetAddress endingAddress) {
        this.tsType = tsType;
        this.ipProtocolId = ipProtocolId;
        this.selectorLength = selectorLength;
        this.startPort = startPort;
        this.endPort = endPort;
        this.startingAddress = startingAddress;
        this.endingAddress = endingAddress;
    }

    /**
     * Construct an instance of IkeTrafficSelector for building an outbound IKE message.
     *
     * @param tsType the Traffic Selector type.
     * @param startPort the smallest port number allowed by this Traffic Selector.
     * @param endPort the largest port number allowed by this Traffic Selector.
     * @param startingAddress the smallest address included in this Traffic Selector.
     * @param endingAddress the largest address included in this Traffic Selector.
     */
    public IkeTrafficSelector(
            @TrafficSelectorType int tsType,
            int startPort,
            int endPort,
            InetAddress startingAddress,
            InetAddress endingAddress) {

        this.tsType = tsType;
        this.ipProtocolId = IP_PROTOCOL_ID_UNSPEC;

        switch (tsType) {
            case TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE:
                this.selectorLength = TRAFFIC_SELECTOR_IPV4_LEN;

                if (!(startingAddress instanceof Inet4Address)
                        || !(endingAddress instanceof Inet4Address)) {
                    throw new IllegalArgumentException(
                            "Invalid address range: TS_IPV4_ADDR_RANGE requires IPv4 addresses.");
                }

                break;
            case TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE:
                throw new UnsupportedOperationException("Do not support IPv6 Traffic Selector.");
                // TODO: Support IPv6 Traffic Selector.
            default:
                throw new IllegalArgumentException("Unrecognized Traffic Selector type.");
        }

        if (!isInetAddressRangeValid(startingAddress, endingAddress)) {
            throw new IllegalArgumentException("Received invalid address range.");
        }

        if (!isPortRangeValid(startPort, endPort)) {
            throw new IllegalArgumentException(
                    "Invalid port range. startPort: "
                            + startPort
                            + " endPort: "
                            + endPort);
        }

        this.startPort = startPort;
        this.endPort = endPort;
        this.startingAddress = startingAddress;
        this.endingAddress = endingAddress;
    }

    // TODO: Add a constructor for users to construct IkeTrafficSelector.

    /**
     * Decode IkeTrafficSelectors from inbound Traffic Selector Payload.
     *
     * <p>This method is only called by IkeTsPayload when decoding inbound IKE message.
     *
     * @param numTs number or Traffic Selectors
     * @param tsBytes encoded byte array of Traffic Selectors
     * @return an array of decoded IkeTrafficSelectors
     * @throws InvalidSyntaxException if received bytes are malformed.
     */
    public static IkeTrafficSelector[] decodeIkeTrafficSelectors(int numTs, byte[] tsBytes)
            throws InvalidSyntaxException {
        IkeTrafficSelector[] tsArray = new IkeTrafficSelector[numTs];
        ByteBuffer inputBuffer = ByteBuffer.wrap(tsBytes);

        try {
            for (int i = 0; i < numTs; i++) {
                int tsType = Byte.toUnsignedInt(inputBuffer.get());
                switch (tsType) {
                    case TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE:
                        tsArray[i] = decodeIpv4TrafficSelector(inputBuffer);
                        break;
                    case TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE:
                        // TODO: Support it.
                        throw new UnsupportedOperationException("Cannot decode this type.");
                    default:
                        throw new InvalidSyntaxException(
                                "Invalid Traffic Selector type: " + tsType);
                }
            }
        } catch (BufferOverflowException e) {
            // Throw exception if any Traffic Selector has invalid length.
            throw new InvalidSyntaxException(e);
        }

        if (inputBuffer.remaining() != 0) {
            throw new InvalidSyntaxException(
                    "Unexpected trailing characters of Traffic Selectors.");
        }

        return tsArray;
    }

    // Decode Traffic Selector using IPv4 address range from a ByteBuffer. A BufferOverflowException
    // will be thrown and caught by method caller if operation reaches the input ByteBuffer's limit.
    private static IkeTrafficSelector decodeIpv4TrafficSelector(ByteBuffer inputBuffer)
            throws InvalidSyntaxException {
        // Decode and validate IP Protocol ID
        int ipProtocolId = Byte.toUnsignedInt(inputBuffer.get());
        if (!IP_PROTOCOL_ID_SET.contains(ipProtocolId)) {
            throw new InvalidSyntaxException("Invalid IP Protocol ID.");
        }

        // Decode and validate Selector Length
        int tsLength = Short.toUnsignedInt(inputBuffer.getShort());
        if (TRAFFIC_SELECTOR_IPV4_LEN != tsLength) {
            throw new InvalidSyntaxException("Invalid Traffic Selector Length.");
        }

        // Decode and validate ports
        int startPort = Short.toUnsignedInt(inputBuffer.getShort());
        int endPort = Short.toUnsignedInt(inputBuffer.getShort());
        if (!isPortRangeValid(startPort, endPort)) {
            throw new InvalidSyntaxException(
                    "Received invalid port range. startPort: "
                            + startPort
                            + " endPort: "
                            + endPort);
        }

        // Decode and validate IPv4 addresses
        byte[] startAddressBytes = new byte[IPV4_ADDR_LEN];
        byte[] endAddressBytes = new byte[IPV4_ADDR_LEN];
        inputBuffer.get(startAddressBytes);
        inputBuffer.get(endAddressBytes);
        try {
            Inet4Address startAddress =
                    (Inet4Address) (Inet4Address.getByAddress(startAddressBytes));
            Inet4Address endAddress = (Inet4Address) (Inet4Address.getByAddress(endAddressBytes));

            // Validate address range.
            if (!isInetAddressRangeValid(startAddress, endAddress)) {
                throw new InvalidSyntaxException("Received invalid IPv4 address range.");
            }

            return new IkeTrafficSelector(
                    TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE,
                    ipProtocolId,
                    TRAFFIC_SELECTOR_IPV4_LEN,
                    startPort,
                    endPort,
                    startAddress,
                    endAddress);
        } catch (ClassCastException | UnknownHostException | IllegalArgumentException e) {
            throw new InvalidSyntaxException(e);
        }
    }

    // TODO: Add a method for decoding IPv6 traffic selector.

    // Validate port range.
    private static boolean isPortRangeValid(int startPort, int endPort) {
        return (startPort >= PORT_NUMBER_MIN
                && startPort <= PORT_NUMBER_MAX
                && endPort >= PORT_NUMBER_MIN
                && endPort <= PORT_NUMBER_MAX
                && startPort <= endPort);
    }

    // Validate address range. Caller must ensure two address are same types.
    // TODO: Consider moving it to the platform code in the future.
    private static boolean isInetAddressRangeValid(
            InetAddress startAddress, InetAddress endAddress) {
        byte[] startAddrBytes = startAddress.getAddress();
        byte[] endAddrBytes = endAddress.getAddress();

        if (startAddrBytes.length != endAddrBytes.length) {
            throw new IllegalArgumentException("Two addresses are different types.");
        }

        for (int i = 0; i < startAddrBytes.length; i++) {
            int unsignedByteStart = Byte.toUnsignedInt(startAddrBytes[i]);
            int unsignedByteEnd = Byte.toUnsignedInt(endAddrBytes[i]);

            if (unsignedByteStart < unsignedByteEnd) {
                return true;
            } else if (unsignedByteStart > unsignedByteEnd) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encode traffic selector to ByteBuffer.
     *
     * <p>This method will be only called by IkeTsPayload for building an outbound IKE message.
     *
     * @param byteBuffer destination ByteBuffer that stores encoded traffic selector.
     */
    public void encodeToByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer
                .put((byte) tsType)
                .put((byte) ipProtocolId)
                .putShort((short) selectorLength)
                .putShort((short) startPort)
                .putShort((short) endPort)
                .put(startingAddress.getAddress())
                .put(endingAddress.getAddress());
    }
}
