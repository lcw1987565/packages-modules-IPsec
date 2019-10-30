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

package com.android.ike.eap.statemachine;

import static com.android.ike.eap.EapAuthenticator.LOG;
import static com.android.ike.eap.message.EapData.EAP_TYPE_AKA_PRIME;
import static com.android.ike.eap.message.simaka.EapAkaTypeData.EAP_AKA_CLIENT_ERROR;
import static com.android.ike.eap.message.simaka.EapSimAkaAttribute.EAP_AT_KDF;
import static com.android.ike.eap.message.simaka.EapSimAkaAttribute.EAP_AT_KDF_INPUT;

import android.content.Context;

import com.android.ike.eap.EapResult;
import com.android.ike.eap.EapSessionConfig.EapAkaPrimeConfig;
import com.android.ike.eap.message.EapData.EapMethod;
import com.android.ike.eap.message.EapMessage;
import com.android.ike.eap.message.simaka.EapAkaPrimeTypeData;
import com.android.ike.eap.message.simaka.EapAkaPrimeTypeData.EapAkaPrimeTypeDataDecoder;
import com.android.ike.eap.message.simaka.EapAkaTypeData;
import com.android.ike.eap.message.simaka.EapSimAkaAttribute;
import com.android.ike.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.ike.eap.message.simaka.EapSimAkaAttribute.AtKdf;
import com.android.ike.eap.message.simaka.EapSimAkaAttribute.AtKdfInput;
import com.android.ike.eap.message.simaka.EapSimAkaTypeData.DecodeResult;
import com.android.internal.annotations.VisibleForTesting;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * EapAkaPrimeMethodStateMachine represents the valid paths possible for the EAP-AKA' protocol.
 *
 * <p>EAP-AKA' sessions will always follow the path:
 *
 * Created --+--> Identity --+--> Challenge  --> Final
 *           |               |
 *           +---------------+
 *
 * <p>Note: If the EAP-Request/AKA'-Challenge message contains an AUTN with an invalid sequence
 * number, the peer will indicate a synchronization failure to the server and a new challenge will
 * be attempted.
 *
 * <p>Note: EAP-Request/Notification messages can be received at any point in the above state
 * machine At most one EAP-AKA'/Notification message is allowed per EAP-AKA' session.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4187">RFC 4187, Extensible Authentication Protocol
 *     for Authentication and Key Agreement (EAP-AKA)</a>
 * @see <a href="https://tools.ietf.org/html/rfc5448">RFC 5448, Improved Extensible Authentication
 *     Protocol Method for 3rd Generation Authentication and Key Agreement (EAP-AKA')</a>
 */
public class EapAkaPrimeMethodStateMachine extends EapAkaMethodStateMachine {
    // EAP-AKA' identity prefix (RFC 5448#3)
    private static final String AKA_PRIME_IDENTITY_PREFIX = "6";
    private static final int SUPPORTED_KDF = 1;

    private final EapAkaPrimeConfig mEapAkaPrimeConfig;
    private final EapAkaPrimeTypeDataDecoder mEapAkaPrimeTypeDataDecoder;

    EapAkaPrimeMethodStateMachine(
            Context context, byte[] eapIdentity, EapAkaPrimeConfig eapAkaPrimeConfig) {
        this(
                context,
                eapIdentity,
                eapAkaPrimeConfig,
                EapAkaPrimeTypeData.getEapAkaPrimeTypeDataDecoder());
    }

    @VisibleForTesting
    protected EapAkaPrimeMethodStateMachine(
            Context context,
            byte[] eapIdentity,
            EapAkaPrimeConfig eapAkaPrimeConfig,
            EapAkaPrimeTypeDataDecoder eapAkaPrimeTypeDataDecoder) {
        super(context, eapIdentity, eapAkaPrimeConfig);
        mEapAkaPrimeConfig = eapAkaPrimeConfig;
        mEapAkaPrimeTypeDataDecoder = eapAkaPrimeTypeDataDecoder;

        transitionTo(new CreatedState());
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_AKA_PRIME;
    }

    @Override
    protected DecodeResult<EapAkaTypeData> decode(byte[] typeData) {
        return mEapAkaPrimeTypeDataDecoder.decode(typeData);
    }

    @Override
    protected String getIdentityPrefix() {
        return AKA_PRIME_IDENTITY_PREFIX;
    }

    @Override
    protected ChallengeState buildChallengeState() {
        return new ChallengeState();
    }

    @Override
    protected ChallengeState buildChallengeState(byte[] identity) {
        return new ChallengeState(identity);
    }

    protected class ChallengeState extends EapAkaMethodStateMachine.ChallengeState {
        private final String mTAG = ChallengeState.class.getSimpleName();

        ChallengeState() {
            super();
        }

        ChallengeState(byte[] identity) {
            super(identity);
        }

        @Override
        protected EapResult handleChallengeAuthentication(
                EapMessage message, EapAkaTypeData eapAkaTypeData) {
            EapAkaPrimeTypeData eapAkaPrimeTypeData = (EapAkaPrimeTypeData) eapAkaTypeData;

            if (!isValidChallengeAttributes(eapAkaPrimeTypeData)) {
                return buildAuthenticationRejectMessage(message.eapIdentifier);
            }
            return null;
        }

        @VisibleForTesting
        boolean isValidChallengeAttributes(EapAkaPrimeTypeData eapAkaPrimeTypeData) {
            Map<Integer, EapSimAkaAttribute> attrs = eapAkaPrimeTypeData.attributeMap;

            if (!attrs.containsKey(EAP_AT_KDF) || !attrs.containsKey(EAP_AT_KDF_INPUT)) {
                return false;
            }

            // TODO(b/143073851): implement KDF resolution specified in RFC 5448#3.2
            // This is safe, as there only exists one defined KDF.
            AtKdf atKdf = (AtKdf) attrs.get(EAP_AT_KDF);
            if (atKdf.kdf != SUPPORTED_KDF) {
                return false;
            }

            AtKdfInput atKdfInput = (AtKdfInput) attrs.get(EAP_AT_KDF_INPUT);
            if (atKdfInput.networkName.length == 0) {
                return false;
            }

            boolean hasMatchingNetworkNames =
                    hasMatchingNetworkNames(
                            mEapAkaPrimeConfig.networkName,
                            new String(atKdfInput.networkName, StandardCharsets.UTF_8));
            return mEapAkaPrimeConfig.allowMismatchedNetworkNames || hasMatchingNetworkNames;
        }

        /**
         * Compares the peer's network name against the server's network name.
         *
         * <p>RFC 5448#3.1 describes how the network names are to be compared: "each name is broken
         * down to the fields separated by colons. If one of the names has more colons and fields
         * than the other one, the additional fields are ignored. The remaining sequences of fields
         * are compared. This algorithm allows a prefix match".
         *
         * @return true iff one network name matches the other, as defined by RC 5448#3.1
         */
        @VisibleForTesting
        boolean hasMatchingNetworkNames(String peerNetworkName, String serverNetworkName) {
            // compare network names according to RFC 5448#3.1
            if (peerNetworkName.isEmpty() || serverNetworkName.isEmpty()) {
                return true;
            }

            String[] peerNetworkNameFields = peerNetworkName.split(":");
            String[] serverNetworkNameFields = serverNetworkName.split(":");
            int numFieldsToCompare =
                    Math.min(peerNetworkNameFields.length, serverNetworkNameFields.length);
            for (int i = 0; i < numFieldsToCompare; i++) {
                if (!peerNetworkNameFields[i].equals(serverNetworkNameFields[i])) {
                    LOG.i(
                            mTAG,
                            "EAP-AKA' network names don't match."
                                    + " Peer: " + LOG.pii(peerNetworkName)
                                    + ", Server: " + LOG.pii(serverNetworkName));
                    return false;
                }
            }

            return true;
        }
    }

    EapAkaPrimeTypeData getEapSimAkaTypeData(AtClientErrorCode clientErrorCode) {
        return new EapAkaPrimeTypeData(EAP_AKA_CLIENT_ERROR, Arrays.asList(clientErrorCode));
    }

    EapAkaPrimeTypeData getEapSimAkaTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        return new EapAkaPrimeTypeData(eapSubtype, attributes);
    }
}