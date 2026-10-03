package io.nut.base.crypto.gpg;

/**
 * Represents a GPG key type, encapsulating the algorithm, key size or curve,
 * and the set of capabilities (encrypt, sign, certify, authenticate) that
 * the key supports.
 *
 * <p>Instances are typically created via {@link GPG#buildKey} and are used
 * during key generation to describe both primary keys and subkeys.</p>
 *
 * @author franci
 */
public class KeyType
{
    /** Capability string for Encrypt+Sign+Certify+Authenticate. */
    public static final String ESCA = GPG.ESCA;

    /** Capability string for Encrypt only. */
    public static final String E    = GPG.E;

    /** Capability string for Sign+Certify+Authenticate. */
    public static final String SCA  = GPG.SCA;

    /**
     * Unique lower-case identifier derived from the algorithm and key size
     * or curve name (e.g. {@code "rsa4096"}, {@code "ed25519"}).
     */
    public final String id;

    /**
     * Algorithm name as expected by GnuPG (e.g. {@code "RSA"}, {@code "ECDSA"},
     * {@code "EDDSA"}, {@code "ECDH"}).
     */
    public final String type;

    /**
     * Key size in bits for classic algorithms (RSA, DSA, ELG).
     * {@code 0} for elliptic-curve algorithms where a curve name is used instead.
     */
    public final int bits;

    /**
     * Elliptic-curve name as expected by GnuPG (e.g. {@code "cv25519"},
     * {@code "nistp256"}), or {@code null} for bit-length-based algorithms.
     */
    public final String curve;

    /**
     * Upper-case capability string composed of any combination of
     * {@code 'E'} (encrypt), {@code 'S'} (sign), {@code 'C'} (certify)
     * and {@code 'A'} (authenticate).
     */
    public final String usage;

    /** {@code true} if this key type supports encryption. */
    public final boolean encrypt;

    /** {@code true} if this key type supports signing. */
    public final boolean sign;

    /** {@code true} if this key type supports certification. */
    public final boolean cert;

    /** {@code true} if this key type supports authentication. */
    public final boolean auth;

    /**
     * Constructs a {@code KeyType} with the given algorithm parameters and
     * capability string.
     *
     * @param type   GnuPG algorithm name (e.g. {@code "RSA"}, {@code "ECDSA"})
     * @param bits   key size in bits; use {@code 0} for curve-based algorithms
     * @param curve  elliptic-curve name, or {@code null} for bit-length algorithms
     * @param usage  capability string (any combination of {@code E}, {@code S},
     *               {@code C}, {@code A}, case-insensitive)
     */
    public KeyType(String type, int bits, String curve, String usage)
    {
        this.id = ((bits>0?type+bits:"")+(curve!=null?curve:"")).toLowerCase();
        this.type   = type;
        this.bits   = bits;
        this.curve  = curve;
        this.usage  = usage = usage.toUpperCase();
        this.encrypt= usage.contains("E");
        this.sign   = usage.contains("S");
        this.cert   = usage.contains("C");
        this.auth   = usage.contains("A");
    }

    /**
     * Returns a human-readable representation of this key type that combines
     * the {@link #id} with the active capability flags.
     *
     * @return string such as {@code "rsa4096ESC"} or {@code "ed25519SC"}
     */
    @Override
    public String toString()
    {
        return id+(encrypt?"E":"")+(sign?"S":"")+(cert?"C":"")+(auth?"A":"");
    }
    
}
