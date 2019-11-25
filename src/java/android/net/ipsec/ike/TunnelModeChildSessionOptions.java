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

package android.net.ipsec.ike;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.LinkAddress;

import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttribute;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Address;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Dhcp;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Dns;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Netmask;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Subnet;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Address;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Dns;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Subnet;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

/**
 * TunnelModeChildSessionOptions represents proposed configurations for negotiating a tunnel mode
 * Child Session.
 *
 * @hide
 */
@SystemApi
public final class TunnelModeChildSessionOptions extends ChildSessionOptions {
    private final ConfigAttribute[] mConfigRequests;

    private TunnelModeChildSessionOptions(
            IkeTrafficSelector[] localTs,
            IkeTrafficSelector[] remoteTs,
            ChildSaProposal[] proposals,
            ConfigAttribute[] configRequests) {
        super(localTs, remoteTs, proposals, false /*isTransport*/);
        mConfigRequests = configRequests;
    }

    /** @hide */
    public ConfigAttribute[] getConfigurationRequests() {
        return mConfigRequests;
    }

    /**
     * This class can be used to incrementally construct a {@link TunnelModeChildSessionOptions}.
     */
    public static final class Builder extends ChildSessionOptions.Builder {
        private static final int IPv4_DEFAULT_PREFIX_LEN = 32;

        private boolean mHasIp4AddressRequest;
        private List<ConfigAttribute> mConfigRequestList;

        /** Create a Builder for negotiating a transport mode Child Session. */
        public Builder() {
            super();
            mHasIp4AddressRequest = false;
            mConfigRequestList = new LinkedList<>();
        }

        /**
         * Adds an Child SA proposal to the {@link TunnelModeChildSessionOptions} being built.
         *
         * @param proposal Child SA proposal.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addSaProposal(@NonNull ChildSaProposal proposal) {
            validateAndAddSaProposal(proposal);
            return this;
        }

        /**
         * Adds an inbound {@link IkeTrafficSelector} to the {@link TunnelModeChildSessionOptions}
         * being built.
         *
         * <p>This method allows callers to limit the inbound traffic transmitted over the Child
         * Session to the given range. the IKE server may further narrow the range. Callers should
         * refer to {@link ChildSessionConfiguration} for the negotiated traffic selectors.
         *
         * <p>If no inbound {@link IkeTrafficSelector} is provided, a default value will be used
         * that covers all IP addresses and ports.
         *
         * @param trafficSelector the inbound {@link IkeTrafficSelector}.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInboundTrafficSelectors(@NonNull IkeTrafficSelector trafficSelector) {
            // TODO: Implement it.
            throw new UnsupportedOperationException("Not yet supported");
        }

        /**
         * Adds an outbound {@link IkeTrafficSelector} to the {@link TunnelModeChildSessionOptions}
         * being built.
         *
         * <p>This method allows callers to limit the outbound traffic transmitted over the Child
         * Session to the given range. the IKE server may further narrow the range. Callers should
         * refer to {@link ChildSessionConfiguration} for the negotiated traffic selectors.
         *
         * <p>If no outbound {@link IkeTrafficSelector} is provided, a default value will be used
         * that covers all IP addresses and ports.
         *
         * @param trafficSelector the outbound {@link IkeTrafficSelector}.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addOutboundTrafficSelectors(@NonNull IkeTrafficSelector trafficSelector) {
            // TODO: Implement it.
            throw new UnsupportedOperationException("Not yet supported");
        }

        /**
         * Adds an internal IP address request to the {@link TunnelModeChildSessionOptions} being
         * built.
         *
         * @param addressFamily the address family. Only {@link OsConstants.AF_INET} and {@link
         *     OsConstants.AF_INET6} are allowed.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalAddressRequest(int addressFamily) {
            if (addressFamily == AF_INET) {
                mHasIp4AddressRequest = true;
                mConfigRequestList.add(new ConfigAttributeIpv4Address());
                return this;
            } else if (addressFamily == AF_INET6) {
                mConfigRequestList.add(new ConfigAttributeIpv6Address());
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Adds a specific internal IP address request to the {@link TunnelModeChildSessionOptions}
         * being built.
         *
         * @param address the requested address.
         * @param prefixLen length of the InetAddress prefix. When requesting an IPv4 address,
         *     prefixLen MUST be 32.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalAddressRequest(@NonNull InetAddress address, int prefixLen) {
            if (address instanceof Inet4Address) {
                if (prefixLen != IPv4_DEFAULT_PREFIX_LEN) {
                    throw new IllegalArgumentException("Invalid IPv4 prefix length: " + prefixLen);
                }
                mHasIp4AddressRequest = true;
                mConfigRequestList.add(new ConfigAttributeIpv4Address((Inet4Address) address));
                return this;
            } else if (address instanceof Inet6Address) {
                mConfigRequestList.add(
                        new ConfigAttributeIpv6Address(new LinkAddress(address, prefixLen)));
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address " + address);
            }
        }

        /**
         * Adds an internal DNS server request to the {@link TunnelModeChildSessionOptions} being
         * built.
         *
         * @param addressFamily the address family. Only {@link OsConstants.AF_INET} and {@link
         *     OsConstants.AF_INET6} are allowed.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalDnsServerRequest(int addressFamily) {
            if (addressFamily == AF_INET) {
                mConfigRequestList.add(new ConfigAttributeIpv4Dns());
                return this;
            } else if (addressFamily == AF_INET6) {
                mConfigRequestList.add(new ConfigAttributeIpv6Dns());
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Adds a specific internal DNS server request to the {@link TunnelModeChildSessionOptions}
         * being built.
         *
         * @param address the requested DNS server address.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalDnsServerRequest(@NonNull InetAddress address) {
            if (address instanceof Inet4Address) {
                mConfigRequestList.add(new ConfigAttributeIpv4Dns((Inet4Address) address));
                return this;
            } else if (address instanceof Inet6Address) {
                mConfigRequestList.add(new ConfigAttributeIpv6Dns((Inet6Address) address));
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address " + address);
            }
        }

        /**
         * Adds an internal subnet requests to the {@link TunnelModeChildSessionOptions} being
         * built.
         *
         * @param addressFamily the address family. Only {@link OsConstants.AF_INET} and {@link
         *     OsConstants.AF_INET6} are allowed.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalSubnetRequest(int addressFamily) {
            if (addressFamily == AF_INET) {
                mConfigRequestList.add(new ConfigAttributeIpv4Subnet());
                return this;
            } else if (addressFamily == AF_INET6) {
                mConfigRequestList.add(new ConfigAttributeIpv6Subnet());
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Adds internal DHCP server requests to the {@link TunnelModeChildSessionOptions} being
         * built.
         *
         * <p>Only DHCPv4 server requests are supported.
         *
         * @param addressFamily the address family. Only {@link OsConstants.AF_INET} is allowed.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalDhcpServerRequest(int addressFamily) {
            if (addressFamily == AF_INET) {
                mConfigRequestList.add(new ConfigAttributeIpv4Dhcp());
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Adds a specific internal DHCP server request to the {@link TunnelModeChildSessionOptions}
         * being built.
         *
         * <p>Only DHCPv4 server requests are supported.
         *
         * @param address the requested DHCP server address.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInternalDhcpServerRequest(@NonNull InetAddress address) {
            if (address instanceof Inet4Address) {
                mConfigRequestList.add(new ConfigAttributeIpv4Dhcp((Inet4Address) address));
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address " + address);
            }
        }

        /**
         * Validates and builds the {@link TunnelModeChildSessionOptions}.
         *
         * @return the validated {@link TunnelModeChildSessionOptions}.
         */
        @NonNull
        public TunnelModeChildSessionOptions build() {
            validateOrThrow();

            if (mHasIp4AddressRequest) {
                mConfigRequestList.add(new ConfigAttributeIpv4Netmask());
            }

            return new TunnelModeChildSessionOptions(
                    mLocalTsList.toArray(new IkeTrafficSelector[mLocalTsList.size()]),
                    mRemoteTsList.toArray(new IkeTrafficSelector[mRemoteTsList.size()]),
                    mSaProposalList.toArray(new ChildSaProposal[mSaProposalList.size()]),
                    mConfigRequestList.toArray(new ConfigAttribute[mConfigRequestList.size()]));
        }
    }
}
