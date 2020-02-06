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

package android.net.ipsec.ike;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseArray;

import com.android.internal.net.ipsec.ike.message.IkePayload;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.DhGroupTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.EncryptionTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.IntegrityTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.PrfTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.Transform;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * SaProposal represents a proposed configuration to negotiate an IKE or Child SA.
 *
 * <p>SaProposal will contain cryptograhic algorithms and key generation materials for the
 * negotiation of an IKE or Child SA.
 *
 * <p>User must provide at least one valid SaProposal when they are creating a new IKE or Child SA.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
@SystemApi
public abstract class SaProposal {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        ENCRYPTION_ALGORITHM_3DES,
        ENCRYPTION_ALGORITHM_AES_CBC,
        ENCRYPTION_ALGORITHM_AES_GCM_8,
        ENCRYPTION_ALGORITHM_AES_GCM_12,
        ENCRYPTION_ALGORITHM_AES_GCM_16
    })
    public @interface EncryptionAlgorithm {}

    /** 3DES Encryption/Ciphering Algorithm. */
    public static final int ENCRYPTION_ALGORITHM_3DES = 3;
    /** AES-CBC Encryption/Ciphering Algorithm. */
    public static final int ENCRYPTION_ALGORITHM_AES_CBC = 12;
    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm with 8-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_8 = 18;
    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm with 12-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_12 = 19;
    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm with 16-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_16 = 20;

    private static final SparseArray<String> SUPPORTED_ENCRYPTION_ALGO_TO_STR;

    static {
        SUPPORTED_ENCRYPTION_ALGO_TO_STR = new SparseArray<>();
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_3DES, "ENCR_3DES");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_CBC, "ENCR_AES_CBC");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_GCM_8, "ENCR_AES_GCM_8");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_GCM_12, "ENCR_AES_GCM_12");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_GCM_16, "ENCR_AES_GCM_16");
    }

    /**
     * Key length unused.
     *
     * <p>This value should only be used with the Encryption/Ciphering Algorithm that accepts a
     * fixed key size such as {@link ENCRYPTION_ALGORITHM_3DES}.
     */
    public static final int KEY_LEN_UNUSED = 0;
    /** AES Encryption/Ciphering Algorithm key length 128 bits. */
    public static final int KEY_LEN_AES_128 = 128;
    /** AES Encryption/Ciphering Algorithm key length 192 bits. */
    public static final int KEY_LEN_AES_192 = 192;
    /** AES Encryption/Ciphering Algorithm key length 256 bits. */
    public static final int KEY_LEN_AES_256 = 256;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        PSEUDORANDOM_FUNCTION_HMAC_SHA1,
        PSEUDORANDOM_FUNCTION_AES128_XCBC,
        PSEUDORANDOM_FUNCTION_SHA2_256,
        PSEUDORANDOM_FUNCTION_SHA2_384,
        PSEUDORANDOM_FUNCTION_SHA2_512
    })
    public @interface PseudorandomFunction {}

    /** HMAC-SHA1 Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_HMAC_SHA1 = 2;
    /** AES128-XCBC Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_AES128_XCBC = 4;
    /** HMAC-SHA2-256 Pseudorandom Function. @hide */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_256 = 5;
    /** HMAC-SHA2-384 Pseudorandom Function. @hide */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_384 = 6;
    /** HMAC-SHA2-384 Pseudorandom Function. @hide */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_512 = 7;

    private static final SparseArray<String> SUPPORTED_PRF_TO_STR;

    static {
        SUPPORTED_PRF_TO_STR = new SparseArray<>();
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_HMAC_SHA1, "PRF_HMAC_SHA1");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_AES128_XCBC, "PRF_AES128_XCBC");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_SHA2_256, "PRF_HMAC2_256");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_SHA2_384, "PRF_HMAC2_384");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_SHA2_512, "PRF_HMAC2_512");
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        INTEGRITY_ALGORITHM_NONE,
        INTEGRITY_ALGORITHM_HMAC_SHA1_96,
        INTEGRITY_ALGORITHM_AES_XCBC_96,
        INTEGRITY_ALGORITHM_HMAC_SHA2_256_128,
        INTEGRITY_ALGORITHM_HMAC_SHA2_384_192,
        INTEGRITY_ALGORITHM_HMAC_SHA2_512_256
    })
    public @interface IntegrityAlgorithm {}

    /** None Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_NONE = 0;
    /** HMAC-SHA1 Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA1_96 = 2;
    /** AES-XCBC-96 Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_AES_XCBC_96 = 5;
    /** HMAC-SHA256 Authentication/Integrity Algorithm with 128-bit truncation. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_256_128 = 12;
    /** HMAC-SHA384 Authentication/Integrity Algorithm with 192-bit truncation. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_384_192 = 13;
    /** HMAC-SHA512 Authentication/Integrity Algorithm with 256-bit truncation. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_512_256 = 14;

    private static final SparseArray<String> SUPPORTED_INTEGRITY_ALGO_TO_STR;

    static {
        SUPPORTED_INTEGRITY_ALGO_TO_STR = new SparseArray<>();
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_NONE, "AUTH_NONE");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_HMAC_SHA1_96, "AUTH_HMAC_SHA1_96");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_AES_XCBC_96, "AUTH_AES_XCBC_96");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(
                INTEGRITY_ALGORITHM_HMAC_SHA2_256_128, "AUTH_HMAC_SHA2_256_128");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(
                INTEGRITY_ALGORITHM_HMAC_SHA2_384_192, "AUTH_HMAC_SHA2_384_192");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(
                INTEGRITY_ALGORITHM_HMAC_SHA2_512_256, "AUTH_HMAC_SHA2_512_256");
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DH_GROUP_NONE, DH_GROUP_1024_BIT_MODP, DH_GROUP_2048_BIT_MODP})
    public @interface DhGroup {}

    /** None Diffie-Hellman Group. */
    public static final int DH_GROUP_NONE = 0;
    /** 1024-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_1024_BIT_MODP = 2;
    /** 2048-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_2048_BIT_MODP = 14;

    private static final SparseArray<String> SUPPORTED_DH_GROUP_TO_STR;

    static {
        SUPPORTED_DH_GROUP_TO_STR = new SparseArray<>();
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_NONE, "DH_NONE");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_1024_BIT_MODP, "DH_1024_BIT_MODP");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_2048_BIT_MODP, "DH_2048_BIT_MODP");
    }

    @IkePayload.ProtocolId private final int mProtocolId;
    private final EncryptionTransform[] mEncryptionAlgorithms;
    private final IntegrityTransform[] mIntegrityAlgorithms;
    private final DhGroupTransform[] mDhGroups;

    /** @hide */
    protected SaProposal(
            @IkePayload.ProtocolId int protocol,
            EncryptionTransform[] encryptionAlgos,
            IntegrityTransform[] integrityAlgos,
            DhGroupTransform[] dhGroups) {
        mProtocolId = protocol;
        mEncryptionAlgorithms = encryptionAlgos;
        mIntegrityAlgorithms = integrityAlgos;
        mDhGroups = dhGroups;
    }

    /**
     * Check if the current SaProposal from the SA responder is consistent with the selected
     * reqProposal from the SA initiator.
     *
     * @param reqProposal selected SaProposal from SA initiator
     * @return if current SaProposal from SA responder is consistent with the selected reqProposal
     *     from SA initiator.
     * @hide
     */
    public boolean isNegotiatedFrom(SaProposal reqProposal) {
        return this.mProtocolId == reqProposal.mProtocolId
                && isTransformSelectedFrom(mEncryptionAlgorithms, reqProposal.mEncryptionAlgorithms)
                && isTransformSelectedFrom(mIntegrityAlgorithms, reqProposal.mIntegrityAlgorithms)
                && isTransformSelectedFrom(mDhGroups, reqProposal.mDhGroups);
    }

    /** Package private */
    static boolean isTransformSelectedFrom(Transform[] selected, Transform[] selectFrom) {
        // If the selected proposal has multiple transforms with the same type, the responder MUST
        // choose a single one.
        if ((selected.length > 1) || (selected.length == 0) != (selectFrom.length == 0)) {
            return false;
        }

        if (selected.length == 0) return true;

        return Arrays.asList(selectFrom).contains(selected[0]);
    }

    /** @hide */
    @IkePayload.ProtocolId
    public int getProtocolId() {
        return mProtocolId;
    }

    /**
     * Gets all proposed encryption algorithms
     *
     * @return A list of Pairs, with the IANA-defined ID for the proposed encryption algorithm as
     *     the first item, and the key length (in bits) as the second.
     */
    @NonNull
    public List<Pair<Integer, Integer>> getEncryptionAlgorithms() {
        final List<Pair<Integer, Integer>> result = new ArrayList<>();
        for (EncryptionTransform transform : mEncryptionAlgorithms) {
            result.add(new Pair(transform.id, transform.getSpecifiedKeyLength()));
        }
        return result;
    }

    /**
     * Gets all proposed integrity algorithms
     *
     * @return A list of the IANA-defined IDs for the proposed integrity algorithms
     */
    @NonNull
    public List<Integer> getIntegrityAlgorithms() {
        final List<Integer> result = new ArrayList<>();
        for (Transform transform : mIntegrityAlgorithms) {
            result.add(transform.id);
        }
        return result;
    }

    /**
     * Gets all proposed Diffie-Hellman groups
     *
     * @return A list of the IANA-defined IDs for the proposed Diffie-Hellman groups
     */
    @NonNull
    public List<Integer> getDhGroups() {
        final List<Integer> result = new ArrayList<>();
        for (Transform transform : mDhGroups) {
            result.add(transform.id);
        }
        return result;
    }

    /** @hide */
    public EncryptionTransform[] getEncryptionTransforms() {
        return mEncryptionAlgorithms;
    }

    /** @hide */
    public IntegrityTransform[] getIntegrityTransforms() {
        return mIntegrityAlgorithms;
    }

    /** @hide */
    public DhGroupTransform[] getDhGroupTransforms() {
        return mDhGroups;
    }

    /** @hide */
    protected List<Transform> getAllTransformsAsList() {
        List<Transform> transformList = new LinkedList<>();

        transformList.addAll(Arrays.asList(mEncryptionAlgorithms));
        transformList.addAll(Arrays.asList(mIntegrityAlgorithms));
        transformList.addAll(Arrays.asList(mDhGroups));

        return transformList;
    }

    /**
     * Return all SA Transforms in this SaProposal to be encoded for building an outbound IKE
     * message.
     *
     * <p>This method should be called by only IKE library.
     *
     * @return Array of Transforms to be encoded.
     * @hide
     */
    public abstract Transform[] getAllTransforms();

    /**
     * This class is an abstract Builder for building a SaProposal.
     *
     * @hide
     */
    protected abstract static class Builder {
        protected static final String ERROR_TAG = "Invalid SA Proposal: ";

        // Use set to avoid adding repeated algorithms.
        protected final Set<EncryptionTransform> mProposedEncryptAlgos = new ArraySet<>();
        protected final Set<PrfTransform> mProposedPrfs = new ArraySet<>();
        protected final Set<IntegrityTransform> mProposedIntegrityAlgos = new ArraySet<>();
        protected final Set<DhGroupTransform> mProposedDhGroups = new ArraySet<>();

        protected boolean mHasAead = false;

        protected static boolean isAead(@EncryptionAlgorithm int algorithm) {
            switch (algorithm) {
                case ENCRYPTION_ALGORITHM_3DES:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_CBC:
                    return false;
                case ENCRYPTION_ALGORITHM_AES_GCM_8:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_GCM_12:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_GCM_16:
                    return true;
                default:
                    // Won't hit here.
                    throw new IllegalArgumentException("Unsupported Encryption Algorithm.");
            }
        }

        protected EncryptionTransform[] buildEncryptAlgosOrThrow() {
            if (mProposedEncryptAlgos.isEmpty()) {
                throw new IllegalArgumentException(
                        ERROR_TAG + "Encryption algorithm must be proposed.");
            }

            return mProposedEncryptAlgos.toArray(
                    new EncryptionTransform[mProposedEncryptAlgos.size()]);
        }

        protected void validateAndAddEncryptAlgo(
                @EncryptionAlgorithm int algorithm, int keyLength) {
            // Construct EncryptionTransform and validate proposed algorithm during
            // construction.
            EncryptionTransform encryptionTransform = new EncryptionTransform(algorithm, keyLength);

            // Validate that only one mode encryption algorithm has been proposed.
            boolean isCurrentAead = isAead(algorithm);
            if (!mProposedEncryptAlgos.isEmpty() && (mHasAead ^ isCurrentAead)) {
                throw new IllegalArgumentException(
                        ERROR_TAG
                                + "Proposal cannot has both normal ciphers "
                                + "and combined-mode ciphers.");
            }
            if (isCurrentAead) mHasAead = true;

            mProposedEncryptAlgos.add(encryptionTransform);
        }

        protected void addIntegrityAlgo(@IntegrityAlgorithm int algorithm) {
            // Construct IntegrityTransform and validate proposed algorithm during
            // construction.
            mProposedIntegrityAlgos.add(new IntegrityTransform(algorithm));
        }

        protected void addDh(@DhGroup int dhGroup) {
            // Construct DhGroupTransform and validate proposed dhGroup during
            // construction.
            mProposedDhGroups.add(new DhGroupTransform(dhGroup));
        }
    }

    /** @hide */
    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(IkePayload.getProtocolTypeString(mProtocolId)).append(": ");

        int len = getAllTransforms().length;
        for (int i = 0; i < len; i++) {
            sb.append(getAllTransforms()[i].toString());
            if (i < len - 1) sb.append("|");
        }

        return sb.toString();
    }

    /**
     * Check if the provided algorithm is a supported encryption algorithm.
     *
     * @param algorithm IKE standard encryption algorithm id.
     * @return true if the provided algorithm is a supported encryption algorithm.
     * @hide
     */
    public static boolean isSupportedEncryptionAlgorithm(@EncryptionAlgorithm int algorithm) {
        return SUPPORTED_ENCRYPTION_ALGO_TO_STR.get(algorithm) != null;
    }

    /**
     * Check if the provided algorithm is a supported pseudorandom function.
     *
     * @param algorithm IKE standard pseudorandom function id.
     * @return true if the provided algorithm is a supported pseudorandom function.
     * @hide
     */
    public static boolean isSupportedPseudorandomFunction(@PseudorandomFunction int algorithm) {
        return SUPPORTED_PRF_TO_STR.get(algorithm) != null;
    }

    /**
     * Check if the provided algorithm is a supported integrity algorithm.
     *
     * @param algorithm IKE standard integrity algorithm id.
     * @return true if the provided algorithm is a supported integrity algorithm.
     * @hide
     */
    public static boolean isSupportedIntegrityAlgorithm(@IntegrityAlgorithm int algorithm) {
        return SUPPORTED_INTEGRITY_ALGO_TO_STR.get(algorithm) != null;
    }

    /**
     * Check if the provided group number is for a supported Diffie-Hellman Group.
     *
     * @param dhGroup IKE standard DH Group id.
     * @return true if the provided number is for a supported Diffie-Hellman Group.
     * @hide
     */
    public static boolean isSupportedDhGroup(@DhGroup int dhGroup) {
        return SUPPORTED_DH_GROUP_TO_STR.get(dhGroup) != null;
    }

    /**
     * Return the encryption algorithm as a String.
     *
     * @hide
     */
    public static String getEncryptionAlgorithmString(int algorithm) {
        if (isSupportedEncryptionAlgorithm(algorithm)) {
            return SUPPORTED_ENCRYPTION_ALGO_TO_STR.get(algorithm);
        }
        return "ENC_Unknown_" + algorithm;
    }

    /**
     * Return the pseudorandom function as a String.
     *
     * @hide
     */
    public static String getPseudorandomFunctionString(int algorithm) {
        if (isSupportedPseudorandomFunction(algorithm)) {
            return SUPPORTED_PRF_TO_STR.get(algorithm);
        }
        return "PRF_Unknown_" + algorithm;
    }

    /**
     * Return the integrity algorithm as a String.
     *
     * @hide
     */
    public static String getIntegrityAlgorithmString(int algorithm) {
        if (isSupportedIntegrityAlgorithm(algorithm)) {
            return SUPPORTED_INTEGRITY_ALGO_TO_STR.get(algorithm);
        }
        return "AUTH_Unknown_" + algorithm;
    }

    /**
     * Return Diffie-Hellman Group as a String.
     *
     * @hide
     */
    public static String getDhGroupString(int dhGroup) {
        if (isSupportedDhGroup(dhGroup)) {
            return SUPPORTED_DH_GROUP_TO_STR.get(dhGroup);
        }
        return "DH_Unknown_" + dhGroup;
    }
}
