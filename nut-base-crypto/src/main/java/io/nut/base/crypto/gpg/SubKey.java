/*
 *  SubKey.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto.gpg;

import io.nut.base.util.Parsers;

/**
 * Represents a single GPG key record as returned by
 * {@code gpg --with-colons} output.
 *
 * <p>A record may describe a primary key ({@code pub}/{@code sec}) or a
 * subkey ({@code sub}/{@code ssb}).  Fields are parsed directly from the
 * colon-delimited line format documented in the GnuPG
 * <a href="https://github.com/gpg/gnupg/blob/master/doc/DETAILS">DETAILS</a>
 * file.</p>
 *
 * <p>After construction, two optional fields may be populated lazily via
 * {@link #setFingerprint(String)} and {@link #setGrp(String)}, which are
 * set from the subsequent {@code fpr} and {@code grp} records.</p>
 *
 * @author franci
 * @see MainKey
 */
public class SubKey
{
    /**
     * Record type as it appears in the colon-delimited output
     * (e.g. {@code "pub"}, {@code "sec"}, {@code "sub"}, {@code "ssb"}).
     */
    public final String type;

    /**
     * Validity flag of the key as defined by GnuPG
     * (e.g. {@code "u"}=ultimate, {@code "f"}=full, {@code "-"}=unknown).
     */
    public final String valid;

    /**
     * Key size in bits (e.g. {@code 4096} for RSA-4096).
     * {@code 0} is used for elliptic-curve keys where size is determined by
     * the curve.
     */
    public final int bits;

    /**
     * Public-key algorithm identifier as defined by OpenPGP
     * (e.g. {@code 1}=RSA, {@code 17}=DSA, {@code 18}=ECDH, {@code 22}=EdDSA).
     */
    public final int algorithm;

    /**
     * Short key ID (last 16 hex characters of the fingerprint) used to
     * identify the key.
     */
    public final String keyid;

    /**
     * Key creation date expressed as a Unix epoch timestamp (seconds since
     * 1970-01-01 00:00:00 UTC).
     */
    public final long createdEpochSecond;

    /**
     * Key expiration date expressed as a Unix epoch timestamp, or {@code 0}
     * if the key does not expire.
     */
    public final long expiresEpochSecond;

    /**
     * Hash of the user ID, or an empty string when not applicable.
     * Used in {@code uid} records to uniquely identify a user ID.
     */
    public final String uid_hash;

    /**
     * Owner-trust character as assigned by GnuPG
     * (e.g. {@code 'u'}=ultimate, {@code 'f'}=full, {@code 0} if absent).
     */
    public final char ownertrust;

    /**
     * Signature class field from the colon record (field 11).
     * Meaning varies by record type; may be empty.
     */
    public final String sigclass;

    /**
     * Capabilities advertised by this key, as a string of letters:
     * {@code E}=encrypt, {@code S}=sign, {@code C}=certify, {@code A}=authenticate.
     * May appear in lowercase for disabled capabilities.
     */
    public final String capabilities;

    /**
     * Timestamp of the last update for this key record as a Unix epoch
     * timestamp, or {@code 0} if not available.
     */
    public final long updateEpochSecond;

    /**
     * Origin of the key (e.g. a key-server URL), or an empty string if
     * not provided.
     */
    public final String origin;

    /** Full 40-character fingerprint; set lazily from the subsequent {@code fpr} record. */
    private volatile String fingerprint;

    /** Keygrip value; set lazily from the subsequent {@code grp} record. */
    private volatile String grp;

    /**
     * Constructs a {@code SubKey} by parsing a colon-delimited field array
     * as produced by {@code gpg --with-colons}.
     *
     * <p>The expected field layout (0-based) follows the GnuPG DETAILS
     * specification:</p>
     * <pre>
     *  0  type
     *  1  validity
     *  2  bits
     *  3  algorithm
     *  4  keyid
     *  5  creation date (epoch)
     *  6  expiration date (epoch, may be empty)
     *  7  uid hash / certificate S/N
     *  8  owner trust
     *  9  (user-id string or empty)
     * 10  sig class
     * 11  capabilities
     * 12  origin (optional)
     * </pre>
     *
     * @param s colon-split field array from a single {@code gpg --with-colons} line
     */
    public SubKey(String[] s)
    {
        this.type = s[0];
        this.valid = s[1];
        this.bits = Integer.parseInt(s[2]);
        this.algorithm = Integer.parseInt(s[3]);
        this.keyid = s[4];
        this.createdEpochSecond = Long.parseLong(s[5]);
        this.expiresEpochSecond = Parsers.safeParseLong(s[6], 0);
        this.uid_hash = s[7];
        this.ownertrust = s[8].isEmpty() ? 0 : s[8].charAt(0);
        this.sigclass = s[10];
        this.capabilities = s[11];
        this.updateEpochSecond = Parsers.safeParseLong(s[11], 0);
        this.origin = s.length<13 ? "" : s[12];
    }

    /**
     * Sets the full fingerprint of this key, obtained from the {@code fpr}
     * record that immediately follows a key record in the
     * {@code gpg --with-colons} output.
     *
     * @param s the 40-character hexadecimal fingerprint string
     */
    void setFingerprint(String s)
    {
        this.fingerprint = s;
    }

    /**
     * Sets the keygrip of this key, obtained from the {@code grp}
     * record that immediately follows a key record in the
     * {@code gpg --with-colons} output.
     *
     * @param s the keygrip string (40 hexadecimal characters)
     */
    void setGrp(String s)
    {
        this.grp = s;
    }

    /**
     * Returns the full 40-character hexadecimal fingerprint of this key,
     * or {@code null} if the corresponding {@code fpr} record has not been
     * parsed yet.
     *
     * @return the fingerprint, or {@code null}
     */
    public String getFingerprint()
    {
        return fingerprint;
    }

    /**
     * Returns the keygrip of this key, or {@code null} if the corresponding
     * {@code grp} record has not been parsed yet.
     *
     * @return the keygrip string, or {@code null}
     */
    public String getGrp()
    {
        return grp;
    }
    
}
