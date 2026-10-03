/*
 *  JPAKE.java
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
 */
package io.nut.base.crypto.pake;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound2Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound3Payload;

/**
 * High-level facade for the J-PAKE (Password-Authenticated Key Exchange by
 * Juggling) protocol built on top of the Bouncy Castle
 * {@link JPAKEParticipant} implementation.
 *
 * <h2>Protocol overview</h2>
 * <p>J-PAKE lets two parties establish a shared session key derived from a
 * common password without ever transmitting the password itself.  The exchange
 * consists of three rounds of messages:
 * <ol>
 *   <li><strong>Round 1</strong> — each side generates ephemeral values and
 *       zero-knowledge proofs, serialised with {@link #createRound1PayloadToSend()}
 *       and consumed with {@link #validateRound1PayloadReceived(String)}.</li>
 *   <li><strong>Round 2</strong> — each side commits its password contribution,
 *       using {@link #createRound2PayloadToSend()} and
 *       {@link #validateRound2PayloadReceived(String)}.</li>
 *   <li><strong>Round 3</strong> — MAC-based key confirmation via
 *       {@link #createRound3PayloadToSend(BigInteger)} and
 *       {@link #validateRound3PayloadReceived(String, BigInteger)}.</li>
 * </ol>
 *
 * <h2>Wire format</h2>
 * <p>Every round payload is serialised to an ASCII string.  Fields are
 * separated by {@value #FS}; multiple {@link BigInteger} values within a
 * single field are joined with {@value #AS}.  All numeric values are encoded
 * as URL-safe Base64 without padding, making the strings safe for email bodies
 * and URLs.
 *
 * <h2>Resumable sessions</h2>
 * <p>When a {@code JPAKE} instance is created with {@code resumable = true},
 * the underlying participant is a {@link ResumableJPAKEParticipant} whose
 * state can be checkpointed via {@link #save()} and restored via
 * {@link #load(String, char[])}.
 *
 * <h2>Session-key derivation</h2>
 * <p>After all three rounds complete successfully, both parties possess the
 * same raw keying material.  Pass it to {@link #deriveSessionKey(BigInteger)}
 * to obtain a ready-to-use AES-256 key via HKDF-SHA256.
 *
 * <h2>Thread safety</h2>
 * <p>Instances are <em>not</em> thread-safe.  The shared {@link SecureRandom}
 * is thread-safe and may be used concurrently by multiple instances.
 */
public class JPAKE
{
    /** Shared {@link SecureRandom} instance used for all ephemeral scalar generation. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Field separator character used between payload fields in the wire format. */
    public static final String FS = "/";

    /** Array-element separator used to join multiple {@link BigInteger} values within a single field. */
    public static final String AS = ";";

    /** Round-1 payload type tag written at the start of every Round-1 message. */
    public static final String R1 = "r1";

    /** Round-2 payload type tag written at the start of every Round-2 message. */
    public static final String R2 = "r2";

    /** Round-3 payload type tag written at the start of every Round-3 message. */
    public static final String R3 = "r3";
    
    // URL-safe Base64 without padding — no '+', '/', '=' → safe for email and URLs
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    /**
     * Encodes one or more {@link BigInteger} values as a single
     * {@value #AS}-separated, URL-safe Base64 string without padding.
     * A {@code null} element is encoded as an empty token so that the
     * array structure is preserved for round-trip decoding.
     *
     * @param values the integers to encode; individual elements may be {@code null}
     * @return a {@value #AS}-separated Base64 string representing all values
     */
    private static String encodeBigInt(BigInteger... values)
    {
        StringJoiner sj = new StringJoiner(AS);
        for(BigInteger item : values)
        {
            String s = (item==null) ? "" : ENC.encodeToString(item.toByteArray());
            sj.add(s);
        }
        return sj.toString();
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
    
    /**
     * Decodes a {@value #AS}-separated string of URL-safe Base64 tokens into
     * an array of {@link BigInteger} values, as produced by
     * {@link #encodeBigInt(BigInteger...)}.  Empty tokens are decoded as
     * {@code null}.
     *
     * @param s the {@value #AS}-delimited Base64 string to decode
     * @return array of decoded {@link BigInteger} values (elements may be {@code null})
     */
    private static BigInteger[] decodeBigInts(String s)
    {
        String[] a = s.split(AS);
        BigInteger[] bi = new BigInteger[a.length];
        for (int i = 0; i < a.length; i++)
        {
            bi[i] = decodeBigInt(a[i]);
        }
        return bi;
    }

    /**
     * Creates a non-resumable {@code JPAKE} instance using the NIST-3072
     * prime-order group.
     *
     * @param participantId unique identifier for this participant (e.g. {@code "Alice"})
     * @param passphrase    shared secret password; must not be empty
     * @return a new {@code JPAKE} instance ready to start Round 1
     */
    public static JPAKE getNIST3072(String participantId, char[] passphrase)
    {
        return new JPAKE(participantId, passphrase, JPAKEPrimeOrderGroups.NIST_3072, false);
    }

    /**
     * Creates a non-resumable {@code JPAKE} instance using the NIST-2048
     * prime-order group.
     *
     * @param participantId unique identifier for this participant
     * @param passphrase    shared secret password; must not be empty
     * @return a new {@code JPAKE} instance ready to start Round 1
     */
    public static JPAKE getNIST2048(String participantId, char[] passphrase)
    {
        return new JPAKE(participantId, passphrase, JPAKEPrimeOrderGroups.NIST_2048, false);
    }

    /**
     * Creates a {@code JPAKE} instance using the NIST-3072 prime-order group,
     * optionally backed by a {@link ResumableJPAKEParticipant} so that the
     * session state can be checkpointed and restored across JVM restarts.
     *
     * @param participantId unique identifier for this participant
     * @param passphrase    shared secret password; must not be empty
     * @param resumable     {@code true} to use a {@link ResumableJPAKEParticipant}
     *                      that supports {@link #save()} / {@link #load(String, char[])};
     *                      {@code false} for a standard in-memory-only participant
     * @return a new {@code JPAKE} instance ready to start Round 1
     */
    public static JPAKE getNIST3072(String participantId, char[] passphrase, boolean resumable)
    {
        return new JPAKE(participantId, passphrase, JPAKEPrimeOrderGroups.NIST_3072, resumable);
    }

    /**
     * Creates a {@code JPAKE} instance using the NIST-2048 prime-order group,
     * optionally backed by a {@link ResumableJPAKEParticipant}.
     *
     * @param participantId unique identifier for this participant
     * @param passphrase    shared secret password; must not be empty
     * @param resumable     {@code true} to use a {@link ResumableJPAKEParticipant};
     *                      {@code false} for a standard in-memory-only participant
     * @return a new {@code JPAKE} instance ready to start Round 1
     */
    public static JPAKE getNIST2048(String participantId, char[] passphrase, boolean resumable)
    {
        return new JPAKE(participantId, passphrase, JPAKEPrimeOrderGroups.NIST_2048, resumable);
    }
  
    /**
     * The underlying Bouncy Castle participant that performs all cryptographic
     * operations.  May be a plain {@link JPAKEParticipant} or a
     * {@link ResumableJPAKEParticipant} depending on how this instance was
     * created.
     */
    public final JPAKEParticipant participant;

    /**
     * Package-private constructor that creates the appropriate participant
     * implementation based on the {@code resumable} flag.
     *
     * <p>The {@code id} is passed through {@link #validateId(String)} before
     * being forwarded to the underlying participant, so that any character that
     * would corrupt the wire format is rejected eagerly with a clear error
     * message rather than causing a silent protocol failure later.
     *
     * @param id        unique participant identifier; must not be null, empty,
     *                  or contain {@value #FS} or whitespace
     * @param passphrase shared secret password
     * @param nist      prime-order group defining the J-PAKE parameters
     * @param resumable {@code true} to create a {@link ResumableJPAKEParticipant}
     * @throws IllegalArgumentException if {@code id} fails validation
     */
    JPAKE(String id, char[] passphrase, JPAKEPrimeOrderGroup nist, boolean resumable)
    {
        if (id == null || id.isEmpty())
        {
            throw new IllegalArgumentException("participantId must not be null or empty");
        }
        if (id.contains(FS))
        {
            throw new IllegalArgumentException("participantId must not contain the field separator '" + FS + "': \"" + id + "\"");
        }
        if (id.chars().anyMatch(Character::isWhitespace))
        {
            throw new IllegalArgumentException("participantId must not contain whitespace: \"" + id + "\"");
        }
        this.participant = resumable ? new ResumableJPAKEParticipant(id, passphrase, nist, new SHA256Digest(), RANDOM)
                                     : new JPAKEParticipant(id, passphrase, nist, new SHA256Digest(), RANDOM);
    }
    /**
     * Package-private constructor that wraps an existing participant, used
     * when restoring a session via {@link #load(String, char[])}.
     *
     * @param participant the already-constructed participant to wrap
     */
    JPAKE(JPAKEParticipant participant)
    {
        this.participant = participant;
    }

    /**
     * Returns the current protocol state of the underlying participant.
     *
     * @return one of the {@code STATE_*} constants defined in
     *         {@link JPAKEParticipant} (e.g. {@link JPAKEParticipant#STATE_INITIALIZED},
     *         {@link JPAKEParticipant#STATE_KEY_CALCULATED}, etc.)
     */
    public int getState()
    {
        return participant.getState();
    }

    /**
     * Computes and returns the raw keying material shared with the remote
     * party after both Round-1 and Round-2 payloads have been exchanged and
     * validated.
     *
     * <p>If the passwords matched, both participants will produce the same
     * value; otherwise the values will differ and Round-3 validation will
     * detect the mismatch.  Pass the result to
     * {@link #deriveSessionKey(BigInteger)} to obtain a usable symmetric key.
     *
     * @return raw keying material as a {@link BigInteger}
     * @throws IllegalStateException if Round-2 has not yet been validated
     */
    public BigInteger calculateKeyingMaterial()
    {
        return participant.calculateKeyingMaterial();
    }

    // ─── Round payload serialization (Base64 over the wire) ──────────────────

    /**
     * Generates the Round-1 payload and serialises it to a wire-format string
     * ready to be transmitted to the remote party.
     *
     * <p>The returned string contains the participant ID, the public ephemeral
     * values {@code gx1} and {@code gx2}, and zero-knowledge proofs for both.
     *
     * @return serialised Round-1 payload string prefixed with {@value #R1}
     * @throws IllegalStateException if Round-1 has already been created
     */
    public String createRound1PayloadToSend()
    {
        return r1encode(participant.createRound1PayloadToSend());
    }

    /**
     * Deserialises the Round-1 wire string received from the remote party and
     * validates the accompanying zero-knowledge proofs.
     *
     * @param r1 the Round-1 wire string produced by the partner's
     *           {@link #createRound1PayloadToSend()}
     * @throws CryptoException          if any zero-knowledge proof fails
     * @throws InvalidParameterException if the string format or type tag is invalid
     * @throws IllegalStateException    if Round-1 validation has already been performed
     */
    public void validateRound1PayloadReceived(String r1) throws CryptoException
    {
        participant.validateRound1PayloadReceived(r1decode(r1));
    }

    /**
     * Generates the Round-2 payload and serialises it to a wire-format string.
     *
     * <p>The payload contains the password-derived public value {@code A} (or
     * {@code B} for the second participant) and its zero-knowledge proof.
     * Round-1 must have been validated before calling this method.
     *
     * @return serialised Round-2 payload string prefixed with {@value #R2}
     * @throws IllegalStateException if Round-2 has already been created or
     *                               Round-1 has not yet been validated
     */
    public String createRound2PayloadToSend()
    {
        return r2encode(participant.createRound2PayloadToSend());
    }

    /**
     * Deserialises the Round-2 wire string received from the remote party and
     * validates its zero-knowledge proof.
     *
     * @param r2 the Round-2 wire string produced by the partner's
     *           {@link #createRound2PayloadToSend()}
     * @throws CryptoException          if the zero-knowledge proof fails
     * @throws InvalidParameterException if the string format or type tag is invalid
     * @throws IllegalStateException    if Round-2 validation has already been
     *                                  performed or Round-1 has not been validated
     */
    public void validateRound2PayloadReceived(String r2) throws CryptoException
    {
        participant.validateRound2PayloadReceived(r2decode(r2));
    }

    /**
     * Generates the Round-3 key-confirmation payload and serialises it to a
     * wire-format string.
     *
     * <p>The payload contains a MAC tag computed over the keying material that
     * proves to the remote party that both sides derived the same key.
     * {@link #calculateKeyingMaterial()} must have been called first.
     *
     * @param keyMaterial the value returned by {@link #calculateKeyingMaterial()}
     * @return serialised Round-3 payload string prefixed with {@value #R3}
     * @throws IllegalStateException if Round-3 has already been created or
     *                               keying material has not been calculated
     */
    public String createRound3PayloadToSend(BigInteger keyMaterial)
    {
        return r3encode(participant.createRound3PayloadToSend(keyMaterial));
    }

    /**
     * Deserialises the Round-3 wire string received from the remote party and
     * verifies the MAC tag against the locally derived keying material.
     *
     * <p>A successful validation confirms that both parties used the same
     * password and arrived at the same session key.
     *
     * @param r3          the Round-3 wire string produced by the partner's
     *                    {@link #createRound3PayloadToSend(BigInteger)}
     * @param keyMaterial the value returned by {@link #calculateKeyingMaterial()}
     * @throws CryptoException          if the MAC tag does not match (password mismatch)
     * @throws InvalidParameterException if the string format or type tag is invalid
     * @throws IllegalStateException    if Round-3 validation has already been
     *                                  attempted or keying material has not been calculated
     */
    public void validateRound3PayloadReceived(String r3, BigInteger keyMaterial) throws CryptoException
    {
        participant.validateRound3PayloadReceived(r3decode(r3), keyMaterial);
    }
    
    /**
     * Serialises a {@link JPAKERound1Payload} to the wire-format string.
     * The string begins with the {@value #R1} tag followed by the participant
     * ID, {@code gx1}, {@code gx2}, and the two zero-knowledge proof arrays,
     * all separated by {@value #FS}.
     *
     * @param r1 the payload to serialise
     * @return wire-format Round-1 string
     */
    private static String r1encode(JPAKERound1Payload r1)
    {
        // fields: participantId, g^x1, g^x2, ZKP[x1]=[gr,b], ZKP[x2]=[gr,b]
        return R1 + FS 
                  + r1.getParticipantId() + FS
                  + encodeBigInt(r1.getGx1()) + FS
                  + encodeBigInt(r1.getGx2()) + FS
                  + encodeBigInt(r1.getKnowledgeProofForX1()) + FS
                  + encodeBigInt(r1.getKnowledgeProofForX2()) + FS;
    }

    /**
     * Serialises a {@link JPAKERound2Payload} to the wire-format string.
     * The string begins with the {@value #R2} tag followed by the participant
     * ID, the password-derived point {@code A}, and the zero-knowledge proof
     * array, all separated by {@value #FS}.
     *
     * @param r2 the payload to serialise
     * @return wire-format Round-2 string
     */
    private static String r2encode(JPAKERound2Payload r2)
    {
        // fields: participantId, A (point derived from the password), ZKP[x2s]=[gr,b]
        return R2 + FS 
                  + r2.getParticipantId() + FS
                  + encodeBigInt(r2.getA()) + FS
                  + encodeBigInt(r2.getKnowledgeProofForX2s()) + FS;
    }

    /**
     * Serialises a {@link JPAKERound3Payload} to the wire-format string.
     * The string begins with the {@value #R3} tag followed by the participant
     * ID and the MAC tag, separated by {@value #FS}.
     *
     * @param r3 the payload to serialise
     * @return wire-format Round-3 string
     */
    private static String r3encode(JPAKERound3Payload r3)
    {
        // fields: participantId, macTag (key confirmation)
        return R3 + FS 
                  + r3.getParticipantId() + FS
                  + encodeBigInt(r3.getMacTag()) + FS;
    }
    
    /**
     * Deserialises a wire-format Round-1 string back into a
     * {@link JPAKERound1Payload}.
     *
     * @param r1 the wire-format string produced by {@link #r1encode}
     * @return the reconstructed Round-1 payload
     * @throws InvalidParameterException if the string has fewer than 6 fields
     *                                   or does not start with the {@value #R1} tag
     */
    private static JPAKERound1Payload r1decode(String r1)
    {
        String[] p = r1.trim().split(FS);
        if(p.length<6)
        {
            throw new InvalidParameterException("r1 has invalid format");
        }
        String tag = p[0];
        if(!tag.equalsIgnoreCase(R1))
        {
            throw new InvalidParameterException("r1 has invalid tag");
        }
        String id = p[1];
        BigInteger gx1 = decodeBigInt(p[2]);
        BigInteger gx2 = decodeBigInt(p[3]);
        BigInteger[] kpX1 = decodeBigInts(p[4]);
        BigInteger[] kpX2 = decodeBigInts(p[5]);
        return new JPAKERound1Payload(id, gx1, gx2, kpX1, kpX2);
    }

    /**
     * Deserialises a wire-format Round-2 string back into a
     * {@link JPAKERound2Payload}.
     *
     * @param r2 the wire-format string produced by {@link #r2encode}
     * @return the reconstructed Round-2 payload
     * @throws InvalidParameterException if the string has fewer than 4 fields
     *                                   or does not start with the {@value #R2} tag
     */
    private static JPAKERound2Payload r2decode(String r2)
    {
        String[] p = r2.trim().split(FS);
        if(p.length<4)
        {
            throw new InvalidParameterException("r2 has invalid format");
        }
        String tag = p[0];
        if(!tag.equalsIgnoreCase(R2))
        {
            throw new InvalidParameterException("r2 has invalid tag");
        }
        String id = p[1];
        BigInteger A = decodeBigInt(p[2]);
        BigInteger[] kpX2 = decodeBigInts(p[3]);
        return new JPAKERound2Payload(id, A, kpX2);
    }

    /**
     * Deserialises a wire-format Round-3 string back into a
     * {@link JPAKERound3Payload}.
     *
     * @param r3 the wire-format string produced by {@link #r3encode}
     * @return the reconstructed Round-3 payload
     * @throws InvalidParameterException if the string has fewer than 3 fields
     *                                   or does not start with the {@value #R3} tag
     */
    private static JPAKERound3Payload r3decode(String r3)
    {
        String[] p = r3.trim().split(FS);
        if(p.length<3)
        {
            throw new InvalidParameterException("r3 has invalid format");
        }
        String tag = p[0];
        if(!tag.equalsIgnoreCase(R3))
        {
            throw new InvalidParameterException("r3 has invalid tag");
        }
        String id = p[1];
        BigInteger mt = decodeBigInt(p[2]);
        return new JPAKERound3Payload(id, mt);
    }
    /**
     * Extracts the sender's participant ID from any serialised round payload
     * string without fully deserialising it.
     *
     * <p>This is useful for routing payloads to the correct local participant
     * when multiple sessions are in progress concurrently.
     *
     * @param round any wire-format round string (Round 1, 2, or 3)
     * @return the participant ID embedded in the payload
     * @throws InvalidParameterException if the string has fewer than 3 fields
     */
    public static String getParticipantId(String round)
    {
        String[] p = round.trim().split(FS);
        if(p.length<3)
        {
            throw new InvalidParameterException("round has invalid format");
        }
        return p[1];
    }

    /**
     * Serialises the complete state of the underlying participant to a compact
     * ASCII string so that the session can be resumed later.
     *
     * <p>This method delegates to {@link ResumableJPAKEParticipant#save()} and
     * returns {@code null} when the underlying participant is a plain
     * (non-resumable) {@link JPAKEParticipant}.
     *
     * <p><strong>Security warning:</strong> the returned string may contain
     * the live password and ephemeral private scalars when the state is earlier
     * than {@link JPAKEParticipant#STATE_KEY_CALCULATED}.  The caller MUST
     * encrypt it before writing to any persistent or transmitted medium.
     *
     * @return serialised participant state, or {@code null} if this instance
     *         is not resumable
     */
    public String save()
    {
        if(participant instanceof ResumableJPAKEParticipant)
        {
            ResumableJPAKEParticipant p = (ResumableJPAKEParticipant) participant;
            return p.save();
        }
        return null;
    }

    /**
     * Reconstructs a {@code JPAKE} instance from a string previously returned
     * by {@link #save()}, wrapping a fully restored
     * {@link ResumableJPAKEParticipant}.
     *
     * <p>If the saved state is earlier than
     * {@link JPAKEParticipant#STATE_KEY_CALCULATED} and the password was not
     * embedded in the saved string, supply it here so that Round-2 operations
     * can proceed.  Pass {@code null} once the password has already been
     * cleared (i.e. for states at or beyond key calculation).
     *
     * @param saved    string previously returned by {@link #save()}; must not
     *                 be {@code null} or empty
     * @param password optional passphrase override; cleared before this method
     *                 returns; may be {@code null} for late-stage states
     * @return a fully restored {@code JPAKE} instance ready to continue the exchange
     * @throws IllegalArgumentException if {@code saved} is null/empty, the
     *                                  format is unrecognised, or a password is
     *                                  required but was not supplied
     */
    public static JPAKE load(String saved, char[] password)
    {
        return new JPAKE(ResumableJPAKEParticipant.load(saved, password));
    }        
    
    // ── Round-payload finder methods ─────────────────────────────────────────

    /**
     * Searches {@code s} for the first substring that matches a serialised
     * round payload produced by this class.
     *
     * <p>The wire format is:
     * <pre>
     *   &lt;tag&gt; ( '/' &lt;field&gt; )+ '/'
     * </pre>
     * where every field is either a participant ID (no '/' allowed) or one or
     * more URL-safe Base64 tokens optionally joined by ';'.  The regex is
     * deliberately anchored to the known structure so that arbitrary text that
     * merely contains the prefix does not produce a false positive.
     *
     * @param tag the round tag ({@value #R1}, {@value #R2}, or {@value #R3})
     * @param s   the string to search (may contain surrounding text)
     * @return the first matching payload substring, or {@code null} if none found
     */
    private static String findRoundPayload(String tag, String s)
    {
        // Breakdown of the pattern for, e.g., tag = "r1":
        //
        //   r1               – literal tag
        //   /[^/\s]+         – '/' + participantId (no slashes, no whitespace)
        //   (?:/[A-Za-z0-9_=;-]+)+ – one or more '/'-prefixed Base64/ZKP fields
        //   /                – trailing slash that ends every encoded payload
        //
        // Base64-URL chars: A-Z a-z 0-9 _ -
        // ZKP fields may contain ';' as the array-element separator.
        // '=' is kept for robustness in case padded Base64 ever appears.
        String pattern = tag + "/[^/\\s]+(?:/[A-Za-z0-9_=;-]+)+/";
        Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(s);
        return m.find() ? m.group() : null;
    }

    /**
     * Searches {@code s} for the first Round-1 payload substring — i.e. the
     * result that the remote party's {@link #createRound1PayloadToSend()} would
     * produce.  The match starts with {@code "r1/"} and contains at least five
     * further slash-delimited Base64/ZKP fields plus a trailing slash.
     *
     * @param s the string to search
     * @return the first {@code "r1/…/"} substring found, or {@code null}
     */
    public static String findRound1Payload(String s)
    {
        return findRoundPayload(R1, s);
    }

    /**
     * Searches {@code s} for the first Round-2 payload substring — i.e. the
     * result that the remote party's {@link #createRound2PayloadToSend()} would
     * produce.  The match starts with {@code "r2/"} and contains at least three
     * further slash-delimited Base64/ZKP fields plus a trailing slash.
     *
     * @param s the string to search
     * @return the first {@code "r2/…/"} substring found, or {@code null}
     */
    public static String findRound2Payload(String s)
    {
        return findRoundPayload(R2, s);
    }

    /**
     * Searches {@code s} for the first Round-3 payload substring — i.e. the
     * result that the remote party's
     * {@link #createRound3PayloadToSend(BigInteger)} would produce.  The match
     * starts with {@code "r3/"} and contains at least two further
     * slash-delimited Base64 fields plus a trailing slash.
     *
     * @param s the string to search
     * @return the first {@code "r3/…/"} substring found, or {@code null}
     */
    public static String findRound3Payload(String s)
    {
        return findRoundPayload(R3, s);
    }

    // ── Common utility methods ────────────────────────────────────────────────
    /**
     * Derives an AES-256 session key from the J-PAKE keying material using
     * HKDF-SHA256. Identical for both participant types.
     *
     * @param keyingMaterial result of {@link #calculateKeyingMaterial()}
     * @return 32-byte session key
     */
    public static byte[] deriveSessionKey(BigInteger keyingMaterial)
    {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(keyingMaterial.toByteArray(), "jpake-salt-v1".getBytes(), "session-key".getBytes()));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, key.length);
        return key;
    }

}
