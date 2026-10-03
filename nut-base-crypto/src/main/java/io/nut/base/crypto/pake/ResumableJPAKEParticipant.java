/*
 *  JPakeParticipant.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 *
 *  ─────────────────────────────────────────────────────────────────────────
 *  Derived from org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant
 *  (MIT licence, © Legion of the Bouncy Castle Inc.)
 *  Source: https://github.com/bcgit/bc-java
 *  ─────────────────────────────────────────────────────────────────────────
 *
 *  This class is a faithful copy of JPAKEParticipant with two additions:
 *
 *    String  save()              — serialises the full mutable state to a
 *                                  URL-safe Base64 string safe for email/files.
 *
 *    static JPakeParticipant
 *            load(String, char[]) — reconstructs an instance from that string.
 *                                   The passphrase is only needed when state <
 *                                   STATE_KEY_CALCULATED (password still live);
 *                                   pass null otherwise.
 *
 *  Format (pipe-separated, every field URL-safe Base64 or plain ASCII int):
 *
 *    <version>|<state>|<participantId>|<partnerParticipantId>|
 *    <groupName>|<p>|<q>|<g>|
 *    <x1>|<x2>|<gx1>|<gx2>|<gx3>|<gx4>|<b>
 *
 *  Fields that are null at save-time are encoded as the single character "-".
 *  Sensitive fields (x1, x2, password) MUST be encrypted by the caller before
 *  writing the result of save() to persistent storage.
 */
package io.nut.base.crypto.pake;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound2Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound3Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKEUtil;
import org.bouncycastle.util.Exceptions;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.StringJoiner;
import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;

/**
 * A serialisable extension of {@link JPAKEParticipant} that allows a J-PAKE
 * exchange to be interrupted, saved, and resumed at a later time or on a
 * different JVM instance.
 *
 * <h2>Overview</h2>
 * <p>J-PAKE (Password-Authenticated Key Exchange by Juggling) is a
 * zero-knowledge protocol in which two parties establish a shared secret
 * derived from a common password without ever transmitting the password
 * itself.  The standard Bouncy Castle {@link JPAKEParticipant} holds all
 * mutable state in memory; if the process dies between rounds the handshake
 * cannot be continued.  This class adds two methods to solve that problem:
 * <ul>
 *   <li>{@link #save()} — serialises the full participant state to a compact,
 *       URL-safe Base64 string suitable for embedding in email bodies, files,
 *       or database columns.</li>
 *   <li>{@link #load(String, char[])} — reconstructs a fully operational
 *       {@code ResumableJPAKEParticipant} from a string produced by
 *       {@link #save()}.</li>
 * </ul>
 *
 * <h2>Serialisation format</h2>
 * <p>Fields are joined with {@value #FIELD_SEP} in the following order:
 * <pre>
 *   version | state | participantId | partnerParticipantId |
 *   p | q | g | x1 | x2 | gx1 | gx2 | gx3 | gx4 | b
 * </pre>
 * {@link BigInteger} values are encoded as URL-safe Base64 without padding.
 * {@code String} fields are stored as plain UTF-8 text.
 * Absent (null) values are stored as an empty token between separators.
 *
 * <h2>Security considerations</h2>
 * <p>Until {@link #calculateKeyingMaterial()} is called, the serialised
 * string contains the raw password bytes and the ephemeral private scalars
 * {@code x1} and {@code x2}.  <strong>The caller MUST encrypt the output of
 * {@link #save()} before writing it to any persistent or transmitted
 * medium.</strong>  After {@link #calculateKeyingMaterial()} the password and
 * private scalars are zeroed out and no longer appear in the saved string.
 *
 * <h2>Thread safety</h2>
 * <p>Instances are <em>not</em> thread-safe.  External synchronisation is
 * required if the same instance is accessed from multiple threads.
 *
 * @see JPAKEParticipant
 * @see JPAKEPrimeOrderGroups
 */
public class ResumableJPAKEParticipant extends JPAKEParticipant
{
    // ── Serialisation constants ───────────────────────────────────────────────

    /** Current save/load format version. Increment on incompatible changes. */
    private static final int    SERIAL_VERSION = 1;

    /** Separator character used between individual fields in the serialised form. */
    private static final String FIELD_SEP = "/";

    // URL-safe Base64 without padding — no '+', '/', '=' → safe for email and URLs
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    /**
     * Encodes a {@code String} field for inclusion in the serialised state.
     * Returns an empty string when the value is {@code null} so that the field
     * separator structure is preserved.
     *
     * @param s the string to encode, or {@code null}
     * @return the original string, or an empty string if {@code null}
     */
    private static String encodeString(String s)
    {
        return s!=null ? s : "";
    }
    /**
     * Decodes a {@code String} field from the serialised state.
     * An empty token (produced by {@link #encodeString} for a {@code null}
     * value) is mapped back to {@code null}.
     *
     * @param s the token read from the serialised form
     * @return the original string, or {@code null} if the token was empty
     */
    private static String decodeString(String s)
    {
        return s.isEmpty() ? null : s;
    }
    
    /**
     * Encodes a {@link BigInteger} as a URL-safe Base64 string without padding.
     * Returns an empty string when the value is {@code null}.
     *
     * @param value the integer to encode, or {@code null}
     * @return URL-safe Base64 representation, or an empty string if {@code null}
     */
    private static String encodeBigInt(BigInteger value)
    {
        return value==null ? "" : ENC.encodeToString(value.toByteArray());
    }
    
    /**
     * Decodes a {@link BigInteger} from a URL-safe Base64 token previously
     * produced by {@link #encodeBigInt}.  An empty token is mapped to
     * {@code null}.
     *
     * @param s the Base64 token, or an empty string representing {@code null}
     * @return the decoded {@link BigInteger}, or {@code null} if the token was empty
     */
    private static BigInteger decodeBigInt(String s)
    {
        return s.isEmpty() ? null : new BigInteger(DEC.decode(s));
    }
    
    // ── Supported group names (extend as needed) ──────────────────────────────

    // =========================================================================
    //  Instance fields  (mirror of JPAKEParticipant)
    // =========================================================================

    private final String participantId;
    /** Cleared to null after calculateKeyingMaterial(). */
    private char[] password;

    private final Digest      digest;
    private final SecureRandom random;

    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;

    private String partnerParticipantId;
    private BigInteger x1;
    private BigInteger x2;
    private BigInteger gx1;
    private BigInteger gx2;
    private BigInteger gx3;
    private BigInteger gx4;
    /** Alice's B or Bob's A — partner's round-2 point. */
    private BigInteger b;

    private int state;

    // =========================================================================
    //  Constructors  (identical signatures to JPAKEParticipant)
    // =========================================================================

    /**
     * Convenience constructor using the NIST-3072 prime-order group,
     * SHA-256 as the digest, and the default {@link SecureRandom} provided
     * by the Bouncy Castle {@link CryptoServicesRegistrar}.
     *
     * @param participantId unique identifier for this participant (e.g. {@code "Alice"})
     * @param password      shared secret password; must not be empty
     */
    public ResumableJPAKEParticipant(String participantId, char[] password)
    {
        this(participantId, password, JPAKEPrimeOrderGroups.NIST_3072, SHA256Digest.newInstance(), CryptoServicesRegistrar.getSecureRandom());
    }

    /**
     * Convenience constructor using SHA-256 as the digest and the default
     * {@link SecureRandom} provided by {@link CryptoServicesRegistrar}.
     *
     * @param participantId unique identifier for this participant
     * @param password      shared secret password; must not be empty
     * @param group         the prime-order group defining the J-PAKE parameters
     */
    public ResumableJPAKEParticipant(String participantId, char[] password, JPAKEPrimeOrderGroup group)
    {
        this(participantId, password, group, SHA256Digest.newInstance(), CryptoServicesRegistrar.getSecureRandom());
    }

    /**
     * Full constructor providing explicit control over all cryptographic
     * parameters.  The signature matches that of {@link JPAKEParticipant} so
     * that existing code can swap in this class with minimal changes.
     *
     * @param participantId unique identifier for this participant; must not be {@code null}
     * @param password      shared secret password; must not be {@code null} or empty
     * @param group         prime-order group defining {@code p}, {@code q}, and {@code g};
     *                      must not be {@code null}
     * @param digest        cryptographic digest used for zero-knowledge proofs and MAC tags;
     *                      must not be {@code null}
     * @param random        source of randomness for ephemeral scalar generation;
     *                      must not be {@code null}
     * @throws IllegalArgumentException if any argument is {@code null} or if
     *                                  {@code password} is empty
     */
    public ResumableJPAKEParticipant(String participantId, char[] password, JPAKEPrimeOrderGroup group, Digest digest, SecureRandom random)
    {
        super(participantId, password, group, digest, random);
        JPAKEUtil.validateNotNull(participantId, "participantId");
        JPAKEUtil.validateNotNull(password,      "password");
        JPAKEUtil.validateNotNull(group,          "group");
        JPAKEUtil.validateNotNull(digest,         "digest");
        JPAKEUtil.validateNotNull(random,         "random");
        if (password.length == 0)
        {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        this.participantId = participantId;
        this.password      = Arrays.copyOf(password, password.length);
        this.p             = group.getP();
        this.q             = group.getQ();
        this.g             = group.getG();
        this.digest        = digest;
        this.random        = random;
        this.state         = STATE_INITIALIZED;
    }

    // =========================================================================
    //  Public API  (identical to JPAKEParticipant)
    // =========================================================================

    /**
     * Returns the current state of this participant.
     *
     * <p>The state advances monotonically through the protocol stages defined
     * in {@link JPAKEParticipant} (e.g. {@link JPAKEParticipant#STATE_INITIALIZED},
     * {@link JPAKEParticipant#STATE_ROUND_1_CREATED}, etc.).
     *
     * @return current protocol state constant
     */
    @Override
    public int getState()
    {
        return state;
    }

    /**
     * Generates the Round-1 payload that this participant must send to the
     * remote party.
     *
     * <p>This method generates the ephemeral private scalars {@code x1} and
     * {@code x2}, computes the corresponding public values {@code gx1} and
     * {@code gx2}, and produces zero-knowledge proofs for both.  It may only
     * be called once per instance; calling it a second time raises
     * {@link IllegalStateException}.
     *
     * @return the Round-1 payload to transmit to the partner
     * @throws IllegalStateException if Round-1 has already been created
     */
    @Override
    public JPAKERound1Payload createRound1PayloadToSend()
    {
        if (state >= STATE_ROUND_1_CREATED)
        {
            throw new IllegalStateException("Round1 payload already created for " + participantId);
        }
        x1  = JPAKEUtil.generateX1(q, random);
        x2  = JPAKEUtil.generateX2(q, random);
        gx1 = JPAKEUtil.calculateGx(p, g, x1);
        gx2 = JPAKEUtil.calculateGx(p, g, x2);

        BigInteger[] kpX1 = JPAKEUtil.calculateZeroKnowledgeProof(p, q, g, gx1, x1, participantId, digest, random);
        BigInteger[] kpX2 = JPAKEUtil.calculateZeroKnowledgeProof(p, q, g, gx2, x2, participantId, digest, random);

        state = STATE_ROUND_1_CREATED;
        return new JPAKERound1Payload(participantId, gx1, gx2, kpX1, kpX2);
    }

    /**
     * Validates the Round-1 payload received from the remote party.
     *
     * <p>Stores the partner's participant ID and their public ephemeral values
     * ({@code gx3}, {@code gx4}), then verifies the accompanying
     * zero-knowledge proofs.
     *
     * @param r1 the Round-1 payload received from the partner; must not be
     *           {@code null}
     * @throws CryptoException       if any zero-knowledge proof fails
     * @throws IllegalStateException if Round-1 validation has already been
     *                               performed, or if the participant IDs are
     *                               identical
     */
    @Override
    public void validateRound1PayloadReceived(JPAKERound1Payload r1) throws CryptoException
    {
        if (state >= STATE_ROUND_1_VALIDATED)
        {
            throw new IllegalStateException("Validation already attempted for round1 payload for " + participantId);
        }
        partnerParticipantId = r1.getParticipantId();
        gx3 = r1.getGx1();
        gx4 = r1.getGx2();

        JPAKEUtil.validateParticipantIdsDiffer(participantId, r1.getParticipantId());
        JPAKEUtil.validateGx4(gx4);
        JPAKEUtil.validateZeroKnowledgeProof(p, q, g, gx3, r1.getKnowledgeProofForX1(), r1.getParticipantId(), digest);
        JPAKEUtil.validateZeroKnowledgeProof(p, q, g, gx4, r1.getKnowledgeProofForX2(), r1.getParticipantId(), digest);

        state = STATE_ROUND_1_VALIDATED;
    }

    /**
     * Generates the Round-2 payload that this participant must send to the
     * remote party.
     *
     * <p>Round-1 validation must have been completed before calling this
     * method.  The method computes the combined public value {@code A} (or
     * {@code B} for the second participant) incorporating the shared password,
     * together with a zero-knowledge proof.
     *
     * @return the Round-2 payload to transmit to the partner
     * @throws IllegalStateException if Round-2 has already been created, or
     *                               if Round-1 has not yet been validated
     */
    @Override
    public JPAKERound2Payload createRound2PayloadToSend()
    {
        if (state >= STATE_ROUND_2_CREATED)
        {
            throw new IllegalStateException("Round2 payload already created for " + participantId);
        }
        if (state < STATE_ROUND_1_VALIDATED)
        {
            throw new IllegalStateException("Round1 payload must be validated prior to creating Round2 payload for " + participantId);
        }
        BigInteger gA   = JPAKEUtil.calculateGA(p, gx1, gx3, gx4);
        BigInteger s    = calculateS();
        BigInteger x2s  = JPAKEUtil.calculateX2s(q, x2, s);
        BigInteger A    = JPAKEUtil.calculateA(p, q, gA, x2s);
        BigInteger[] kp = JPAKEUtil.calculateZeroKnowledgeProof(p, q, gA, A, x2s, participantId, digest, random);

        state = STATE_ROUND_2_CREATED;
        return new JPAKERound2Payload(participantId, A, kp);
    }

    /**
     * Validates the Round-2 payload received from the remote party.
     *
     * <p>Verifies the zero-knowledge proof accompanying the partner's public
     * value and stores the value for use in {@link #calculateKeyingMaterial()}.
     *
     * @param r2 the Round-2 payload received from the partner; must not be
     *           {@code null}
     * @throws CryptoException       if the zero-knowledge proof fails
     * @throws IllegalStateException if Round-2 validation has already been
     *                               performed, or if Round-1 has not yet been
     *                               validated
     */
    @Override
    public void validateRound2PayloadReceived(JPAKERound2Payload r2) throws CryptoException
    {
        if (state >= STATE_ROUND_2_VALIDATED)
        {
            throw new IllegalStateException("Validation already attempted for round2 payload for " + participantId);
        }
        if (state < STATE_ROUND_1_VALIDATED)
        {
            throw new IllegalStateException("Round1 payload must be validated prior to validating Round2 payload for " + participantId);
        }
        BigInteger gB = JPAKEUtil.calculateGA(p, gx3, gx1, gx2);
        b = r2.getA();

        JPAKEUtil.validateParticipantIdsDiffer(participantId, r2.getParticipantId());
        JPAKEUtil.validateParticipantIdsEqual(partnerParticipantId, r2.getParticipantId());
        JPAKEUtil.validateGa(gB);
        JPAKEUtil.validateZeroKnowledgeProof(p, q, gB, b, r2.getKnowledgeProofForX2s(), r2.getParticipantId(), digest);

        state = STATE_ROUND_2_VALIDATED;
    }

    /**
     * Computes and returns the keying material shared with the remote party.
     *
     * <p>Both participants must call this method independently after Round-2
     * validation.  If the passwords match, both will produce the same keying
     * material; otherwise the values will differ and subsequent Round-3
     * validation will detect the mismatch.
     *
     * <p>After this method returns, the password char-array is zeroed and set
     * to {@code null}, and the ephemeral private scalars {@code x1}, {@code x2},
     * and {@code b} are also cleared from memory.
     *
     * @return the raw keying material as a {@link BigInteger}; apply a suitable
     *         key-derivation function (e.g. HKDF) before use as a symmetric key
     * @throws IllegalStateException if the key has already been calculated, or
     *                               if Round-2 has not yet been validated
     */
    @Override
    public BigInteger calculateKeyingMaterial()
    {
        if (state >= STATE_KEY_CALCULATED)
        {
            throw new IllegalStateException("Key already calculated for " + participantId);
        }
        if (state < STATE_ROUND_2_VALIDATED)
        {
            throw new IllegalStateException("Round2 payload must be validated prior to creating key for " + participantId);
        }
        BigInteger s = calculateS();

        // Clear password — no longer needed
        Arrays.fill(password, (char) 0);
        password = null;

        BigInteger keyingMaterial = JPAKEUtil.calculateKeyingMaterial(p, q, gx4, x2, s, b);

        // Clear ephemeral private scalars
        x1 = null;
        x2 = null;
        b  = null;

        state = STATE_KEY_CALCULATED;
        return keyingMaterial;
    }

    /**
     * Generates the Round-3 payload containing the MAC tag that proves to the
     * remote party that both sides derived the same keying material.
     *
     * <p>{@link #calculateKeyingMaterial()} must have been called before
     * invoking this method.
     *
     * @param keyingMaterial the value returned by {@link #calculateKeyingMaterial()}
     * @return the Round-3 payload to transmit to the partner
     * @throws IllegalStateException if Round-3 has already been created, or if
     *                               keying material has not yet been calculated
     */
    @Override
    public JPAKERound3Payload createRound3PayloadToSend(BigInteger keyingMaterial)
    {
        if (state >= STATE_ROUND_3_CREATED)
        {
            throw new IllegalStateException("Round3 payload already created for " + participantId);
        }
        if (state < STATE_KEY_CALCULATED)
        {
            throw new IllegalStateException("Keying material must be calculated prior to creating Round3 payload for " + participantId);
        }
        BigInteger macTag = JPAKEUtil.calculateMacTag(participantId, partnerParticipantId, gx1, gx2, gx3, gx4, keyingMaterial, digest);

        state = STATE_ROUND_3_CREATED;
        return new JPAKERound3Payload(participantId, macTag);
    }

    /**
     * Validates the Round-3 payload received from the remote party.
     *
     * <p>Verifies the partner's MAC tag against the locally derived keying
     * material.  A successful validation confirms that both sides used the
     * same password and derived the same key.
     *
     * <p>After this method returns, the public ephemeral values ({@code gx1}
     * through {@code gx4}) are cleared from memory.
     *
     * @param r3            the Round-3 payload received from the partner
     * @param keyingMaterial the value returned by {@link #calculateKeyingMaterial()}
     * @throws CryptoException       if the MAC tag does not match (passwords differ)
     * @throws IllegalStateException if Round-3 validation has already been
     *                               attempted, or if keying material has not
     *                               yet been calculated
     */
    @Override
    public void validateRound3PayloadReceived(JPAKERound3Payload r3, BigInteger keyingMaterial) throws CryptoException
    {
        if (state >= STATE_ROUND_3_VALIDATED)
        {
            throw new IllegalStateException("Validation already attempted for round3 payload for " + participantId);
        }
        if (state < STATE_KEY_CALCULATED)
        {
            throw new IllegalStateException("Keying material must be calculated prior to validating Round3 payload for " + participantId);
        }
        JPAKEUtil.validateParticipantIdsDiffer(participantId, r3.getParticipantId());
        JPAKEUtil.validateParticipantIdsEqual(partnerParticipantId, r3.getParticipantId());
        JPAKEUtil.validateMacTag(participantId, partnerParticipantId, gx1, gx2, gx3, gx4, keyingMaterial, digest, r3.getMacTag());

        // Clear public ephemerals
        gx1 = null;
        gx2 = null;
        gx3 = null;
        gx4 = null;

        state = STATE_ROUND_3_VALIDATED;
    }

    // =========================================================================
    //  Persistence  — save() / load()
    // =========================================================================

    /**
     * Serialises the complete mutable state of this participant into a
     * compact, ASCII-safe string.
     *
     * <p>The format is {@value #SERIAL_VERSION}-versioned and pipe-separated.
     * Each {@link BigInteger} field is encoded as URL-safe Base64 without
     * padding; null fields are encoded as {@code "-"}.  String fields
     * (participant ids) are UTF-8 byte-arrays in URL-safe Base64 to avoid
     * any issues with special characters.
     *
     * <p><strong>Security warning:</strong> when the state is earlier than
     * {@link #STATE_KEY_CALCULATED}, the serialised string contains the live
     * {@code password} char-array AND the ephemeral private scalars {@code x1}
     * and {@code x2}.  The caller MUST encrypt the result before writing it to
     * any persistent or transmitted medium.
     *
     * @return opaque serialised state string
     */
    public String save()
    {
        StringJoiner sj = new StringJoiner(FIELD_SEP);

        // Header
        sj.add(Integer.toString(SERIAL_VERSION));   // 0 – format version
        sj.add(Integer.toString(state));            // 1 – state
        sj.add(encodeString(participantId));        // 2
        sj.add(encodeString(partnerParticipantId)); // 3  (may be null)

        sj.add(encodeBigInt(p));  // 4
        sj.add(encodeBigInt(q));  // 5
        sj.add(encodeBigInt(g));  // 6

        // Private scalars (sensitive — caller must encrypt)
        sj.add(encodeBigInt(x1));   // 7
        sj.add(encodeBigInt(x2));   // 8

        // Public ephemeral values
        sj.add(encodeBigInt(gx1));  // 9
        sj.add(encodeBigInt(gx2));  // 10
        sj.add(encodeBigInt(gx3));  // 11
        sj.add(encodeBigInt(gx4));  // 12
        sj.add(encodeBigInt(b));    // 13

        return sj.toString();
    }

    /**
     * Reconstructs a {@link ResumableJPAKEParticipant} from a string previously
     * returned by {@link #save()}.
     *
     * <p>If the saved state is earlier than {@link #STATE_KEY_CALCULATED} AND
     * the {@code password} field was cleared before saving (i.e. the caller
     * chose not to persist it for security reasons), supply the passphrase
     * here so that round-2 operations can proceed.  In all other cases
     * pass {@code null}.
     *
     * <p>The {@code passphrase} array is cleared before this method returns.
     *
     * @param saved      string returned by {@link #save()}
     * @param password optional passphrase override (see above); may be null
     * @return fully restored participant ready to continue the exchange
     * @throws IllegalArgumentException if the format is unrecognised
     */
    public static ResumableJPAKEParticipant load(String saved, char[] password)
    {
        if (saved == null || saved.isEmpty())
        {
            throw new IllegalArgumentException("saved state must not be null or empty");
        }

        String[] fields = saved.split(FIELD_SEP, -1);  // -1 keeps trailing empties
        if (fields.length < 13)
        {
            throw new IllegalArgumentException("Invalid saved state: expected 13 fields, got " + fields.length);
        }

        int version = Integer.parseInt(fields[0]);
        if (version != SERIAL_VERSION)
        {
            throw new IllegalArgumentException("Unsupported save version: " + version);
        }

        int    savedState         = Integer.parseInt(fields[1]);
        String savedParticipantId = decodeString(fields[2]);
        String savedPartnerId     = decodeString(fields[3]);   // may be null

        BigInteger p     = decodeBigInt(fields[4]);
        BigInteger q     = decodeBigInt(fields[5]);
        BigInteger g     = decodeBigInt(fields[6]);

        BigInteger savedX1  = decodeBigInt(fields[7]);
        BigInteger savedX2  = decodeBigInt(fields[8]);
        BigInteger savedGx1 = decodeBigInt(fields[9]);
        BigInteger savedGx2 = decodeBigInt(fields[10]);
        BigInteger savedGx3 = decodeBigInt(fields[11]);
        BigInteger savedGx4 = decodeBigInt(fields[12]);
        BigInteger savedB   = decodeBigInt(fields[13]);

        // If state < KEY_CALCULATED we need a password
        if(savedState < STATE_KEY_CALCULATED && password==null)
        {
            throw new IllegalArgumentException("State " + savedState + " requires a password");
        }

        JPAKEPrimeOrderGroup group = resolveGroup(p,q,g); 

        ResumableJPAKEParticipant instance = new ResumableJPAKEParticipant(savedParticipantId, password, group, SHA256Digest.newInstance(), CryptoServicesRegistrar.getSecureRandom());

        // Restore all fields
        instance.state               = savedState;
        instance.partnerParticipantId= savedPartnerId;
        instance.x1                  = savedX1;
        instance.x2                  = savedX2;
        instance.gx1                 = savedGx1;
        instance.gx2                 = savedGx2;
        instance.gx3                 = savedGx3;
        instance.gx4                 = savedGx4;
        instance.b                   = savedB;

        if(savedState >= STATE_KEY_CALCULATED && instance.password!=null)
        {
            Arrays.fill(password, (char) 0);
            password = null;
        }
        if (password != null)
        {
            Arrays.fill(password, (char) 0);
        }

        return instance;
    }

    /**
     * Computes the password-derived scalar {@code s} used during Round-2.
     * Wraps {@link JPAKEUtil#calculateS} and re-throws any checked
     * {@link CryptoException} as an {@link IllegalStateException}.
     *
     * @return the password scalar {@code s} modulo {@code q}
     * @throws IllegalStateException if the underlying calculation fails
     */
    private BigInteger calculateS()
    {
        try
        {
            return JPAKEUtil.calculateS(q, password);
        }
        catch (CryptoException e)
        {
            throw Exceptions.illegalStateException(e.getMessage(), e);
        }
    }

    private static final JPAKEPrimeOrderGroup[] PRIME_ORDER_GROUPS = { JPAKEPrimeOrderGroups.NIST_3072, JPAKEPrimeOrderGroups.NIST_2048, JPAKEPrimeOrderGroups.SUN_JCE_1024};
    
    /**
     * Resolves the {@link JPAKEPrimeOrderGroup} that matches the given
     * prime-order parameters.
     *
     * <p>The method first tries each of the well-known groups defined in
     * {@link JPAKEPrimeOrderGroups} (NIST-3072, NIST-2048, SUN-JCE-1024).
     * If none matches, a custom {@link JPAKEPrimeOrderGroup} is constructed
     * directly from the supplied parameters.
     *
     * @param p the prime modulus
     * @param q the prime divisor of {@code p - 1}
     * @param g the generator of the prime-order subgroup
     * @return the matching well-known group, or a new custom group if none matches
     */
    private static JPAKEPrimeOrderGroup resolveGroup(BigInteger p, BigInteger q, BigInteger g)
    {
        for(JPAKEPrimeOrderGroup item : PRIME_ORDER_GROUPS)
        {
            if(p.equals(item.getP())&& q.equals(item.getQ()) && g.equals(item.getG()))
            {
                return item;
            }
        }
        return new JPAKEPrimeOrderGroup(p, q, g);
    }
}
