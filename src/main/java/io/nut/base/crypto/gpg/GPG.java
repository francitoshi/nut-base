/*
 *  GPG.java
 *
 *  Copyright (c) 2025-2026 francitoshi@gmail.com
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

import io.nut.base.io.IO;
import io.nut.base.io.VerboseLineReader;
import io.nut.base.util.Args;
import io.nut.base.util.BashEscaper;
import io.nut.base.util.Byter;
import io.nut.base.util.Strings;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//https://github.com/gpg/gnupg/blob/master/doc/DETAILS

/**
 * Facade for the GnuPG ({@code gpg}) command-line tool.
 *
 * <p>Each public method of this class spawns a {@code gpg} child process,
 * communicates with it through its standard streams, and returns a
 * Java-friendly result.  The class is <em>not</em> thread-safe by default;
 * configuration setters ({@link #setDebug}, {@link #setArmor}, etc.) should
 * be called before any operation methods are invoked from concurrent threads.</p>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * GPG gpg = new GPG().setArmor(true);
 * byte[] cipher = gpg.encryptAndSign(plaintext, signerFingerprint, passphrase, recipientFingerprint);
 * byte[] plain  = gpg.decryptAndVerify(cipher, passphrase, null);
 * }</pre>
 *
 * <p>See the GnuPG
 * <a href="https://github.com/gpg/gnupg/blob/master/doc/DETAILS">DETAILS</a>
 * document for the colon-delimited output format used by key-listing
 * operations.</p>
 *
 * @author franci
 */
public class GPG
{   

    // ALGORITHMS
    
    private static final String RSA = "RSA";     //1024-4096 bits
    private static final String DSA = "DSA";     //768-3072 bits
    private static final String ELG = "ELG";     //1024-4096 bits
    private static final String ECDSA = "ECDSA";
    private static final String EDDSA = "EDDSA";
    private static final String ECDH = "ECDH";

    // CURVES  
    
    public static final String CURVE25519 = "curve25519";
    public static final String SECP256K1 = "secp256k1";
    public static final String BRAINPOOLP512R1 = "brainpoolP512r1";
    public static final String BRAINPOOLP384R1 = "brainpoolP384r1";
    public static final String BRAINPOOLP256R1 = "brainpoolP256r1";
    public static final String NISTP256 = "nistp256";
    public static final String NISTP384 = "nistp384";
    public static final String NISTP521 = "nistp521";
    public static final String ELG4096 = "elg4096";
    public static final String ELG3072 = "elg3072";
    public static final String ELG2048 = "elg2048";
    public static final String ELG1024 = "elg1024";
    public static final String DSA3072 = "dsa3072";
    public static final String DSA2048 = "dsa2048";
    public static final String DSA1024 = "dsa1024";
    public static final String RSA4096 = "rsa4096";
    public static final String RSA3072 = "rsa3072";
    public static final String RSA2048 = "rsa2048";
    public static final String RSA1024 = "rsa1024";
    
    // CAPABILITIES
    
    public static final String ESCA = "ESCA";
    public static final String E    = "E";
    public static final String ES   = "ES";
    public static final String S    = "S";
    public static final String SCA  = "SCA";
    public static final String CA   = "CA";
    public static final String SC   = "SC";
    
    // OPTIONS
    
    private static final String BATCH = "--batch";
    private static final String YES   = "--yes";
    private static final String NOTTY = "--no-tty";
    private static final String ARMOR = "--armor";
    private static final String COMMENT = "--comment";
    private static final String EMIT_VERSION = "--emit-version";
    private static final String OUTPUT = "--output";
    private static final String VERBOSE = "--verbose";

    private static final String STATUS_FD_1 = "--status-fd=1";
    private static final String STATUS_FD_2 = "--status-fd=2";

    private static final String PINENTRYMODE_CANCEL = "--pinentry-mode=cancel";
    private static final String PINENTRYMODE_LOOPBACK = "--pinentry-mode=loopback";

    private static final String PASSPHRASE_FD_0 = "--passphrase-fd=0";


    // OTHER
    
    private static final int BUFFER_SIZE = 8*1024;

    private static final Pattern GPG_ARMOR_HEADER_PATTERN = Pattern.compile("gpg: armor header: (.+): (.*)");
    
    private volatile boolean debug;
    private volatile boolean armor;
    private volatile boolean emitVersion;
    private volatile String comment;

    private BufferedReader debugger(BufferedReader src)
    {
        return this.debug ? new VerboseLineReader(src, System.out) : src;
    }

    private static class GnuPG
    {
        final boolean debug;
        volatile boolean mergeOutErr;
        final Args args = new Args();
        public GnuPG(boolean debug, String... params)
        {
            this.debug = debug;
            this.mergeOutErr = false;
            this.args.add(params);
        }
        public GnuPG add(String... params)
        {
            args.add(params);
            return this;
        }
        public GnuPG add(boolean include, String... params)
        {
            args.add(include, params);
            return this;
        }
        public GnuPG merge()
        {
            mergeOutErr = true;
            return this;
        }
        public Process start() throws IOException
        {
            final ProcessBuilder pb = new ProcessBuilder("gpg");
            pb.command().addAll(args.get());
            pb.redirectErrorStream(mergeOutErr);

            if(debug)
            {
                System.out.println(BashEscaper.buildCommandLine(pb.command()));
            }
            return pb.start();
        }
    }

    
    /**
     * Enables or disables debug output.
     *
     * <p>When debug mode is active, every GPG command line is printed to
     * {@code System.out} before execution, and lines that are not explicitly
     * handled by parsing logic are echoed to {@code System.err}.</p>
     *
     * @param value {@code true} to enable debug output; {@code false} to disable
     * @return this instance, for method chaining
     */
    public GPG setDebug(boolean value)
    {
        this.debug = value;
        return this;
    }
    /**
     * Enables or disables ASCII-armor output for encryption and signing
     * operations.
     *
     * <p>When enabled, the {@code --armor} flag is passed to GPG so that
     * the output is Base64-encoded and suitable for inclusion in plain-text
     * protocols (e-mail, etc.).</p>
     *
     * @param value {@code true} to produce ASCII-armored output; {@code false}
     *              for binary output
     * @return this instance, for method chaining
     */
    public GPG setArmor(boolean value)
    {
        this.armor = value;
        return this;
    }

    /**
     * Enables or disables the emission of a {@code Version:} header in
     * ASCII-armored output.
     *
     * <p>When enabled, the {@code --emit-version} flag is passed to GPG.
     * Has no effect when {@link #setArmor(boolean) armor} is disabled.</p>
     *
     * @param value {@code true} to include the version header; {@code false}
     *              to omit it
     * @return this instance, for method chaining
     */
    public GPG setEmitVersion(boolean value)
    {
        this.emitVersion = value;
        return this;
    }

    /**
     * Sets a comment string to be embedded in the ASCII-armor header of
     * encrypted or signed output.
     *
     * <p>When non-{@code null}, the {@code --comment} flag is passed to GPG.
     * Has no effect when {@link #setArmor(boolean) armor} is disabled.</p>
     *
     * @param comment the comment string, or {@code null} to omit the header
     * @return this instance, for method chaining
     */
    public GPG setComment(String comment)
    {
        this.comment = comment;
        return this;
    }

    
    private GnuPG gpg(String... params) throws IOException 
    {
        return new GnuPG(debug, params);
    }
    
    /**
     * Parses the colon-delimited output of {@code gpg --with-colons} and
     * populates the supplied lists with public and/or secret key entries.
     *
     * <p>Each top-level {@code pub} record starts a new {@link PubKey} that is
     * appended to {@code pubKeys}; each {@code sec} record starts a new
     * {@link SecKey} appended to {@code secKeys}.  Subsequent {@code sub},
     * {@code ssb}, {@code uid}, {@code fpr}, and {@code grp} records are
     * associated with the most recently opened key entry.</p>
     *
     * <p>Either list may be {@code null} if the caller is only interested in
     * one key type.</p>
     *
     * @param in      input stream containing the {@code gpg --with-colons} output;
     *                must not be {@code null}
     * @param pubKeys list to which parsed {@link PubKey} instances are added,
     *                or {@code null} to skip public keys
     * @param secKeys list to which parsed {@link SecKey} instances are added,
     *                or {@code null} to skip secret keys
     */
    public void parseKeys(InputStream in, List<PubKey> pubKeys, List<SecKey> secKeys)
    {
        Scanner sc = new Scanner(in);
        SubKey key=null;
        MainKey grp=null;
        PubKey pub=null;
        SecKey sec=null;
        while(sc.hasNext())
        {
            String line = sc.nextLine();
            String[] s = line.split(":");
            switch(line.substring(0, 3))
            {
                case "tru":
                    break;
                case "pub":
                    pubKeys.add(pub=new PubKey(key=new SubKey(s)));
                    sec=null;
                    grp=pub;
                    break;
                case "sec":
                    secKeys.add(sec=new SecKey(key=new SubKey(s)));
                    pub=null;
                    grp=sec;
                    break;
                case "sub":
                    pub.add(new SubKey(s));
                    break;
                case "ssb":
                    sec.add(new SubKey(s));
                    break;
                case "uid":
                    grp.add(new UserId(s));
                    break;
                case "fpr":
                    key.setFingerprint(s[9]);
                    break;
                case "grp":
                    key.setGrp(s[9]);
                    break;
                default:
                    if(debug) System.err.println(line);
            }
        }
    }
    
    /**
     * Returns public-key entries from the local GPG keyring, optionally
     * filtered by one or more search terms.
     *
     * <p>Equivalent to running {@code gpg --list-public-keys --with-colons
     * [lookfor...]}.  When no search terms are given, all public keys are
     * returned.</p>
     *
     * @param lookfor zero or more key identifiers (fingerprint, key ID, e-mail,
     *                or name) used to restrict the result set
     * @return array of matching {@link PubKey} entries; never {@code null},
     *         may be empty
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the GPG process to finish
     */
    public PubKey[] getPubKeys(String... lookfor) throws IOException, InterruptedException
    {
        List<PubKey> pubs = new ArrayList<>();
        Process process = gpg(BATCH, "--list-public-keys","--with-colons").add(lookfor).start();
        parseKeys(process.getInputStream(), pubs, null);
        process.waitFor();
        return pubs.toArray(new PubKey[0]);
    }
    
    /**
     * Returns secret-key entries from the local GPG keyring, optionally
     * filtered by one or more search terms.
     *
     * <p>Equivalent to running {@code gpg --list-secret-keys --with-colons
     * [lookfor...]}.  When no search terms are given, all secret keys are
     * returned.</p>
     *
     * @param lookfor zero or more key identifiers (fingerprint, key ID, e-mail,
     *                or name) used to restrict the result set
     * @return array of matching {@link SecKey} entries; never {@code null},
     *         may be empty
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the GPG process to finish
     */
    public SecKey[] getSecKeys(String... lookfor) throws IOException, InterruptedException
    {
        List<SecKey> secs = new ArrayList<>();
        Process process = gpg(BATCH, "--list-secret-keys","--with-colons").add(lookfor).start();
        parseKeys(process.getInputStream(), null, secs);
        process.waitFor();
        return secs.toArray(new SecKey[0]);
    }

    /**
     * Deletes the public key identified by {@code name} from the local keyring.
     *
     * <p>Equivalent to running {@code gpg --batch --yes --delete-keys <name>}.
     * The {@code name} may be a fingerprint, key ID, e-mail address, or any
     * other identifier accepted by GPG.</p>
     *
     * @param name the key identifier to delete; must not be {@code null}
     * @return the GPG process exit code: {@code 0} on success, non-zero on error
     * @throws IOException          if the GPG process cannot be started
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the GPG process to finish
     */
    public int deletePubKeys(String name) throws IOException, InterruptedException
    {
        return gpg(BATCH, YES,"--delete-keys",name).start().waitFor();
    }
    
    /**
     * Deletes the secret key identified by {@code fingerprint} from the local
     * keyring.
     *
     * <p>Equivalent to running
     * {@code gpg --batch --yes --delete-secret-keys <fingerprint>}.</p>
     *
     * @param fingerprint the full fingerprint of the secret key to delete;
     *                    must not be {@code null}
     * @return the GPG process exit code: {@code 0} on success, non-zero on error
     * @throws IOException          if the GPG process cannot be started
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the GPG process to finish
     */
    public int deleteSecKeys(String fingerprint) throws IOException, InterruptedException
    {
        return gpg(BATCH, YES,"--delete-secret-keys", fingerprint).start().waitFor();
    }
    
    /**
     * Deletes both the secret key and the corresponding public key identified
     * by {@code fingerprint} from the local keyring in a single operation.
     *
     * <p>Equivalent to running
     * {@code gpg --batch --yes --delete-secret-and-public-key <fingerprint>}.</p>
     *
     * @param fingerprint the full fingerprint of the key pair to delete;
     *                    must not be {@code null}
     * @return the GPG process exit code: {@code 0} on success, non-zero on error
     * @throws IOException          if the GPG process cannot be started
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the GPG process to finish
     */
    public int deleteSecAndPubKeys(String fingerprint) throws IOException, InterruptedException
    {
        return gpg(BATCH, YES,"--delete-secret-and-public-key", fingerprint).start().waitFor();
    }
    
    /**
     * Converts a compact capability string into the comma-separated usage
     * string expected by the GPG batch key-generation script.
     *
     * <p>For example, {@code "SCA"} becomes {@code "sign,cert,auth"} and
     * {@code "E"} becomes {@code "encrypt"}.</p>
     *
     * @param usages a string containing any combination of the characters
     *               {@code 'c'}/{@code 'C'} (cert), {@code 'e'}/{@code 'E'}
     *               (encrypt), {@code 's'}/{@code 'S'} (sign) and
     *               {@code 'a'}/{@code 'A'} (auth); case-insensitive
     * @return comma-separated GPG usage string (e.g. {@code "sign,encrypt"});
     *         never {@code null}, may be empty if no recognised character is found
     */
    public static String usage(String usages)
    {
        usages = usages.toLowerCase();
        StringJoiner sj = new StringJoiner(",");
        if(usages.indexOf('c')>=0)
        {
            sj.add("cert");
        }
        if(usages.indexOf('e')>=0)
        {
            sj.add("encrypt");
        }
        if(usages.indexOf('s')>=0)
        {
            sj.add("sign");
        }
        if(usages.indexOf('a')>=0)
        {
            sj.add("auth");
        }
        return sj.toString();
    }
    
    /**
     * https://www.gnupg.org/documentation/manuals/gnupg/Unattended-GPG-key-generation.html
     * @param keyType rsa4096, nistp521, curve25519
     * @param keyUsage C=cert E=encrypt S=sign A=auth
     * @param nameReal real name of the owner
     * @param comment
     * @param email email of the owner
     * @param passphrase passphrase to encrypt the key
     * @param expire 0 (never), Nd (days), Nw (weeks), Nm (months), Ny (years)
     * @return 0 on success or GPG error codes on error
     * @throws IOException
     * @throws InterruptedException
     */
    public int genKey(String keyType, String keyUsage, String nameReal, String comment, String email, String passphrase, String expire) throws IOException, InterruptedException
    {
        return genKey(keyType, keyUsage, null, null, nameReal, comment, email, passphrase, expire);
    }    

    /**
     * https://www.gnupg.org/documentation/manuals/gnupg/Unattended-GPG-key-generation.html
     * @param keyType rsa4096, nistp521, curve25519
     * @param keyUsage C=cert E=encrypt S=sign A=auth
     * @param ssbType rsa4096, nistp521, curve25519
     * @param ssbUsage C=cert E=encrypt S=sign A=auth
     * @param nameReal real name of the owner
     * @param comment
     * @param email email of the owner
     * @param passphrase passphrase to encrypt the key
     * @param expire 0 (never), Nd (days), Nw (weeks), Nm (months), Ny (years)
     * @return 0 on success or GPG error codes on error
     * @throws IOException
     * @throws InterruptedException
     */
    public int genKey(String keyType, String keyUsage, String ssbType, String ssbUsage, String nameReal, String comment, String email, String passphrase, String expire) throws IOException, InterruptedException
    {
        KeyType secKey = buildKey(keyType, keyUsage);
        KeyType ssbKey = ssbType!=null ? buildKey(ssbType, ssbUsage) : null;
        
        Process process = gpg(BATCH, "--generate-key").start();
        
        try(PrintStream out = new PrintStream(process.getOutputStream()))
        {
            out.println("%no-ask-passphrase");
            out.println("Key-Type: "+secKey.type);
            if(secKey.bits>0) 
            {
                out.println("Key-Length: "+secKey.bits);
            }
            if(secKey.curve!=null) 
            {
                out.println("Key-Curve: "+secKey.curve);
            }
            out.println("Key-Usage: "+usage(secKey.usage)); // usage: encrypt, sign, cert, auth

            if(ssbKey!=null)
            {
                out.println("Subkey-Type: "+ssbKey.type);
                if(ssbKey.bits>0) 
                {
                    out.println("Subkey-Length: "+ssbKey.bits);
                }
                if(ssbKey.curve!=null) 
                {
                    out.println("Subkey-Curve: "+ssbKey.curve);
                }
                out.println("Subkey-Usage: "+usage(ssbKey.usage));
            }
            if(nameReal!=null && !Strings.isBlank(nameReal))
            {
                out.println("Name-Real: " + nameReal);
            }
            if(comment!=null && !Strings.isBlank(comment))
            {
                out.println("Name-Comment: "+comment);
            }
            out.println("Name-Email: " + email);
            out.println("Expire-Date: "+expire); // 
            out.println("Passphrase: " + passphrase);
            out.println("Preferences: SHA512 SHA384 SHA256 AES256 AES192 AES ZLIB BZIP2 ZIP");
            out.println("%commit");
            out.println("%echo done");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) 
        {
            try (Scanner sc = new Scanner(process.getInputStream())) 
            {
                while(sc.hasNext())
                {
                    System.err.println(sc.nextLine());
                }
            }
        }
        return exitCode;
    }
    
    /**
     * Adds an RSA subkey to an existing key via interactive key editing.
     *
     * @param keyId      identifier of the primary key to edit (fingerprint,
     *                   key ID, or e-mail)
     * @param ssbBits    RSA key size in bits (e.g. {@code 2048}, {@code 4096})
     * @param sign       {@code true} to grant signing capability
     * @param encrypt    {@code true} to grant encryption capability
     * @param auth       {@code true} to grant authentication capability
     * @param expire     expiration spec: {@code "0"} for no expiry,
     *                   or a value such as {@code "1y"}, {@code "6m"}, {@code "30d"}
     * @param passphrase passphrase protecting the primary key, or {@code null}
     *                   if no passphrase is set
     * @return the GPG process exit code: {@code 0} on success, non-zero on error
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted
     */
    public int addKeyRSA(String keyId, int ssbBits, boolean sign, boolean encrypt, boolean auth, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, true, false, false, false, ssbBits, sign, encrypt, auth,  0, expire, passphrase);
    }
    /**
     * Adds a DSA subkey to an existing key via interactive key editing.
     *
     * <p>DSA only supports signing and authentication; encryption is not
     * available for this algorithm.</p>
     *
     * @param keyId      identifier of the primary key to edit
     * @param ssbBits    DSA key size in bits (768–3072)
     * @param sign       {@code true} to grant signing capability
     * @param auth       {@code true} to grant authentication capability
     * @param expire     expiration spec (see {@link #addKeyRSA} for format)
     * @param passphrase passphrase protecting the primary key, or {@code null}
     * @return the GPG process exit code: {@code 0} on success, non-zero on error
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted
     */
    public int addKeyDSA(String keyId, int ssbBits, boolean sign, boolean auth, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, false, true, false, false, ssbBits, sign, false, auth,  0, expire, passphrase);
    }
    /**
     * Adds an Elgamal (encryption-only) subkey to an existing key via
     * interactive key editing.
     *
     * @param keyId      identifier of the primary key to edit
     * @param ssbBits    Elgamal key size in bits (1024–4096)
     * @param expire     expiration spec (see {@link #addKeyRSA} for format)
     * @param passphrase passphrase protecting the primary key, or {@code null}
     * @return the GPG process exit code: {@code 0} on success, non-zero on error
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted
     */
    public int addKeyELG(String keyId, int ssbBits, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, false, false, true, false, ssbBits, false, true, false,  0, expire, passphrase);
    }

    /**
    * Adds an ECC (Elliptic Curve Cryptography) subkey to an existing key.
    *
    * <p>This method uses {@code gpg --expert --edit-key} in interactive mode,
    * simulating user input to add a new ECC subkey. The {@code ssbCurve} parameter
    * represents the numeric selection of the elliptic curve as presented by GnuPG
    * in its interactive menu.</p>
    *
    * <h3>ECC capabilities</h3>
    * <ul>
    *   <li>{@code sign=true} → signing key (ECDSA or EdDSA)</li>
    *   <li>{@code encrypt=true} → encryption key (ECDH)</li>
    *   <li>{@code auth=true} → authentication key</li>
    * </ul>
    *
    * <p>Not all combinations are valid. GnuPG restrictions apply:</p>
    * <ul>
    *   <li>Signing and encryption cannot be combined in ECC subkeys</li>
    *   <li>Encryption-only → ECDH</li>
    *   <li>Signing/auth → ECDSA or EdDSA depending on curve</li>
    * </ul>
    *
    * <h3>Curve selection ({@code ssbCurve})</h3>
    * <p>The value corresponds to the menu index shown by GnuPG when selecting
    * an ECC curve. Typical values (GnuPG 2.2+/2.4+) are:</p>
    *
    * <table border="1">
    *   <tr><th>Value</th><th>Curve</th><th>Usage</th></tr>
    *   <tr><td>1</td><td>Curve25519</td><td>Encryption (ECDH), recommended</td></tr>
    *   <tr><td>2</td><td>NIST P-256</td><td>Signing / Authentication</td></tr>
    *   <tr><td>3</td><td>NIST P-384</td><td>Signing / Authentication</td></tr>
    *   <tr><td>4</td><td>NIST P-521</td><td>Signing / Authentication</td></tr>
    *   <tr><td>5</td><td>BrainpoolP256r1</td><td>Signing / Authentication</td></tr>
    *   <tr><td>6</td><td>BrainpoolP384r1</td><td>Signing / Authentication</td></tr>
    *   <tr><td>7</td><td>BrainpoolP512r1</td><td>Signing / Authentication</td></tr>
    *   <tr><td>8</td><td>secp256k1</td><td>Signing (less commonly used in GPG)</td></tr>
    * </table>
    *
    * <p><b>Note:</b> These numeric values are not part of a stable API and may vary
    * depending on the installed GnuPG version and configuration.</p>
    *
    * <h3>Recommended usage</h3>
    * <ul>
    *   <li>Encryption: {@code ssbCurve = 1} (Curve25519)</li>
    *   <li>Signing: Ed25519 (implicitly selected via Curve25519 in signing mode)</li>
    * </ul>
    *
    * @param keyId       ID, fingerprint, or email of the key to modify
    * @param sign        whether the subkey should be used for signing
    * @param encrypt     whether the subkey should be used for encryption
    * @param auth        whether the subkey should be used for authentication
    * @param ssbCurve    numeric identifier of the elliptic curve (see table above)
    * @param expire      expiration date (e.g. {@code "0"}, {@code "1y"}, {@code "6m"})
    * @param passphrase  passphrase for the secret key (may be {@code null} or empty)
    *
    * @return 0 on success, or a GPG error code on failure
    *
    * @throws IOException if the GPG process fails or I/O errors occur
    * @throws InterruptedException if the process is interrupted
    * @throws InvalidParameterException if an invalid capability combination is used
    */
    public int addKeyECC(String keyId, boolean sign, boolean encrypt, boolean auth, int ssbCurve, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, false, false, false, true, 0, sign, encrypt, auth, ssbCurve, expire, passphrase);
    }
    private int editKeyAddKey(String keyId, boolean rsa, boolean dsa, boolean elg, boolean ecc, int ssbBits, boolean sign, boolean encrypt, boolean auth, int ssbCurve, String expire, String passphrase) throws IOException, InterruptedException
    {
        boolean pass = passphrase!=null && !passphrase.isEmpty();
        
        Process process = gpg(BATCH,"--expert", YES, "--command-fd=0")
                .add(pass, PASSPHRASE_FD_0, PINENTRYMODE_LOOPBACK)
                .add("--edit-key", keyId).start();
        try(PrintStream out = new PrintStream(process.getOutputStream()))
        {
            if(pass)
            {
                out.println(passphrase);
            }
            out.println("addkey");
            String ops;
            if(rsa)
            {
                ops = getRsaOps(sign, encrypt, auth);
            }
            else if(dsa)
            {
                ops = getDsaOps(sign, auth);
            }
            else if(elg)
            {
                ops = "5";
            }
            else if(ecc)
            {
                ops = getEccOps(sign, encrypt, auth);
            }
            else
            {
                throw new InvalidParameterException("no algorithm");
            }
            for(String item : ops.split(","))
            {
                out.println(item);
            }

            if(rsa||dsa||elg) 
            {
                out.println(ssbBits);
            } 
            else if(ecc)
            {
                out.println(ssbCurve); // curve25519
            }
            
            out.println(expire);
            out.println("save");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) 
        {
            try (Scanner sc = new Scanner(process.getInputStream())) {
                while(sc.hasNext())
                {
                    System.err.println(sc.nextLine());
                }
            }
        }
        return exitCode;
    }

    /**
     * Returns the comma-separated sequence of GPG interactive-menu answers
     * needed to create an RSA subkey with the requested capabilities.
     *
     * <p>The returned string is intended to be split on {@code ','} and fed
     * line by line to the {@code gpg --edit-key} command-fd interface.</p>
     *
     * @param sign    {@code true} if the subkey should have signing capability
     * @param encrypt {@code true} if the subkey should have encryption capability
     * @param auth    {@code true} if the subkey should have authentication capability
     * @return the GPG menu-answer sequence (e.g. {@code "8,A,Q"})
     * @throws java.security.InvalidParameterException if no capability is requested
     */
    public String getRsaOps(boolean sign, boolean encrypt, boolean auth) throws InvalidParameterException
    {
        String ops;
        if(sign && encrypt && auth)
        {
            ops = "8,A,Q";
        }
        else if(sign && encrypt)
        {
            ops = "8,Q";
        }
        else if(sign && auth)
        {
            ops = "8,E,A,Q";
        }
        else if(sign)
        {
            ops = "4";
        }
        else if(encrypt && auth)
        {
            ops = "8,S,A,Q";
        }
        else if(encrypt)
        {
            ops = "6";
        }
        else if(auth)
        {
            ops = "8,S,E,A,Q";
        }
        else
        {
            throw new InvalidParameterException("no caps");
        }
        return ops;
    }

    /**
     * Returns the comma-separated sequence of GPG interactive-menu answers
     * needed to create a DSA subkey with the requested capabilities.
     *
     * <p>DSA only supports signing and authentication; requesting encryption
     * is not valid and the parameter is intentionally absent.</p>
     *
     * @param sign {@code true} if the subkey should have signing capability
     * @param auth {@code true} if the subkey should have authentication capability
     * @return the GPG menu-answer sequence (e.g. {@code "8,Q"})
     * @throws java.security.InvalidParameterException if no capability is requested
     */
    public String getDsaOps(boolean sign, boolean auth) throws InvalidParameterException
    {
        String ops;
        if(sign && auth)
        {
            ops = "8,A,Q";
        }
        else if(sign)
        {
            ops = "8,Q";
        }
        else if(auth)
        {
            ops = "8,S,A,Q";
        }
        else
        {
            throw new InvalidParameterException("no caps");
        }
        return ops;
    }

    /**
     * Returns the comma-separated sequence of GPG interactive-menu answers
     * needed to create an ECC subkey with the requested capabilities.
     *
     * <p>ECC keys cannot combine signing and encryption in the same subkey.
     * Attempting to request both will throw an
     * {@link java.security.InvalidParameterException}.</p>
     *
     * @param sign    {@code true} if the subkey should have signing capability
     * @param encrypt {@code true} if the subkey should have encryption capability
     *                (mutually exclusive with {@code sign})
     * @param auth    {@code true} if the subkey should have authentication capability
     * @return the GPG menu-answer sequence (e.g. {@code "10"} for sign-only)
     * @throws java.security.InvalidParameterException if an incompatible or
     *         empty capability combination is requested
     */
    public String getEccOps(boolean sign, boolean encrypt, boolean auth) throws InvalidParameterException
    {
        String ops;
        if(sign && encrypt && auth)
        {
            throw new InvalidParameterException("incompatible caps");
        }
        else if(sign && encrypt)
        {
            throw new InvalidParameterException("incompatible caps");
        }
        else if(sign && auth)
        {
            ops = "11,Q";
        }
        else if(sign)
        {
            ops = "10";
        }
        else if(encrypt && auth)
        {
            throw new InvalidParameterException("incompatible caps");
        }
        else if(encrypt)
        {
            ops = "12";
        }
        else if(auth)
        {
            ops = "11,Q";
        }
        else
        {
            throw new InvalidParameterException("no caps");
        }
        return ops;
    }
    
    /**
     * Prints a summary of all public and secret keys in the local keyring to
     * {@code System.out}.
     *
     * <p>For each key entry, every subkey is printed on its own line in the
     * format {@code "<type> <bits> <capabilities>"} (e.g.
     * {@code "pub 4096 ESCA"}).</p>
     *
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the GPG process to finish
     */
    public void printKeys() throws IOException, InterruptedException
    {
        for(PubKey p : this.getPubKeys())
        {
            SubKey[] keys = p.getKeys();
            for(SubKey k : keys)
            {
                System.out.printf("%s %d %s\n",k.type, k.bits, k.capabilities);
            }
            System.out.println();
        }
        
        for(SecKey p : this.getSecKeys())
        {
            SubKey[] keys = p.getKeys();
            for(SubKey k : keys)
            {
                System.out.printf("%s %d %s\n",k.type, k.bits, k.capabilities);
            }
            System.out.println();
        }
        
    }
        
    private static KeyType buildKey(String keyType, String keyUsage)
    {
        //keyType  = keyType.toLowerCase();
        keyUsage = keyUsage.toUpperCase();
        
        switch (keyType)
        {
            case RSA1024:
                return new KeyType(RSA, 1024, null, keyUsage);
            case RSA2048:
                return new KeyType(RSA, 2048, null, keyUsage);
            case RSA3072:
                return new KeyType(RSA, 3072, null, keyUsage);
            case RSA4096:
                return new KeyType(RSA, 4096, null, keyUsage);

            case DSA1024:
                return new KeyType(DSA, 1024, null, keyUsage);
            case DSA2048:
                return new KeyType(DSA, 2048, null, keyUsage);
            case DSA3072:
                return new KeyType(DSA, 3072, null, keyUsage);
                
            case ELG1024:
                return new KeyType(ELG, 1024, null, keyUsage);
            case ELG2048:
                return new KeyType(ELG, 2048, null, keyUsage);
            case ELG3072:
                return new KeyType(ELG, 3072, null, keyUsage);
            case ELG4096:
                return new KeyType(ELG, 4096, null, keyUsage);
                
            case NISTP256:
            case NISTP384:
            case NISTP521:
            case BRAINPOOLP256R1:
            case BRAINPOOLP384R1:
            case BRAINPOOLP512R1:
            case SECP256K1:
                if(keyUsage.equals(E))
                    return new KeyType(ECDH, 0, keyType, keyUsage);
                else if(!keyUsage.contains("E"))
                    return new KeyType(ECDSA, 0, keyType, keyUsage);
                throw new InvalidParameterException(keyType+" can't be used for "+keyUsage+" in the same key");

            case CURVE25519:
                if(keyUsage.equals(E))
                    return new KeyType(ECDH, 0, "cv25519", keyUsage);
                else if(!keyUsage.contains("E"))
                    return new KeyType(EDDSA, 0, "ed25519", keyUsage);
                throw new InvalidParameterException(keyType+" can't be used for "+keyUsage+" in the same key");
            
            default:
                throw new InvalidParameterException("invalid type "+keyType);
        }
    }
    
    /**
     * Encrypts and optionally signs a byte array using GPG.
     *
     * @param plaindata Array of bytes to encrypt.
     * @param signer Signer ID (fingerprint, short ID, email, or null to not sign).
     * @param recipients Array of recipient IDs (fingerprint, short ID, email).
     * @param passphrase Passphrase for the signer's key (or null to not use).
     * @return Byte array with the encrypted and signed content (if signerId is
     * not null).
     * @throws IOException If GPG or I/O fails.
     * @throws InterruptedException If the GPG process is interrupted.
     */
    public byte[] encryptAndSign(byte[] plaindata, String signer, char[] passphrase, String... recipients) throws IOException, InterruptedException
    {
        if (plaindata == null || plaindata.length == 0)
        {
            throw new IllegalArgumentException("data is null or empty");
        }
        return encryptAndSign(new ByteArrayInputStream(plaindata), signer, passphrase, recipients);
    }
    public byte[] encryptAndSign(InputStream plaindata, String signer, char[] passphrase, String... recipients) throws IOException, InterruptedException
    {
        if (recipients == null || recipients.length == 0 || Arrays.stream(recipients).anyMatch(id -> id == null || id.trim().isEmpty()))
        {
            throw new IllegalArgumentException("Los recipientIds no pueden ser nulos, vacíos o contener elementos inválidos.");
        }
        
        boolean pass = passphrase!=null && passphrase.length!=0;
        
        GnuPG gnupg = gpg(BATCH, NOTTY)
                .add(armor, ARMOR).add(emitVersion, EMIT_VERSION)
                .add(comment!=null, COMMENT, comment)
                .add("--encrypt", OUTPUT, "-");
        for (String recipientId : recipients)
        {
            gnupg.add("--recipient", recipientId);
        }
        if (signer != null && !signer.trim().isEmpty())
        {
            gnupg.add("--sign", "--local-user", signer);
        }
        if(pass)
        {
            gnupg.add(PASSPHRASE_FD_0, PINENTRYMODE_LOOPBACK);
        }
        Process process = gnupg.start();

        // Send passphrase (if applicable) and details
        try (OutputStream stdin = process.getOutputStream())
        {
            if(pass)
            {
                stdin.write(Byter.bytesUTF8(passphrase));
                stdin.write('\n');
            }
            IO.copy(plaindata,stdin);
        }

        // Read encrypted output
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IO.copy(process.getInputStream(), output);

        // Capture errors
        StringBuilder errorOutput = new StringBuilder();
        try (InputStream stderr = process.getErrorStream())
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = stderr.read(buffer)) != -1)
            {
                errorOutput.append(new String(buffer, 0, bytesRead));
            }
        }

        // Check result
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new IOException("Error al cifrar/firmar con GPG. Código: " + exitCode
                    + "\nError: " + errorOutput);
        }

        return output.toByteArray();
    }

    /**
     * Signs a byte array using the specified GPG key.
     *
     * @param plaindata  data to sign; must not be {@code null} or empty
     * @param signer     key identifier of the signing key (fingerprint, key ID,
     *                   or e-mail); must not be {@code null}
     * @param passphrase passphrase protecting the signing key, or {@code null}
     *                   if the key has no passphrase
     * @return byte array containing the GPG signature (binary or ASCII-armored
     *         depending on {@link #setArmor(boolean)})
     * @throws IOException          if the GPG process cannot be started, an I/O
     *                              error occurs, or GPG returns a non-zero exit code
     * @throws InterruptedException if the calling thread is interrupted
     */
    public byte[] sign(byte[] plaindata, String signer, char[] passphrase) throws IOException, InterruptedException
    {
        if (plaindata == null || plaindata.length == 0)
        {
            throw new IllegalArgumentException("data is null or empty");
        }
        return sign(new ByteArrayInputStream(plaindata), signer, passphrase);
    }
    /**
     * Signs the data read from an {@link InputStream} using the specified GPG key.
     *
     * @param plaindata  stream providing the data to sign; must not be {@code null}
     * @param signer     key identifier of the signing key (fingerprint, key ID,
     *                   or e-mail); must not be {@code null}
     * @param passphrase passphrase protecting the signing key, or {@code null}
     *                   if the key has no passphrase
     * @return byte array containing the GPG signature (binary or ASCII-armored
     *         depending on {@link #setArmor(boolean)})
     * @throws IOException          if the GPG process cannot be started, an I/O
     *                              error occurs, or GPG returns a non-zero exit code
     * @throws InterruptedException if the calling thread is interrupted
     */
    public byte[] sign(InputStream plaindata, String signer, char[] passphrase) throws IOException, InterruptedException
    {
        boolean pass = passphrase!=null && passphrase.length!=0;
        
        GnuPG gnupg = gpg(BATCH, NOTTY)
                .add(armor, ARMOR).add(emitVersion, EMIT_VERSION)
                .add(comment!=null, COMMENT, comment)
                .add("--sign", "--local-user", signer)
                .add(OUTPUT, "-");
        if(pass)
        {
            gnupg.add(PASSPHRASE_FD_0, PINENTRYMODE_LOOPBACK);
        }
        Process process = gnupg.start();

        // Send passphrase (if applicable) and details
        try (OutputStream stdin = process.getOutputStream())
        {
            if(pass)
            {
                stdin.write(Byter.bytesUTF8(passphrase));
                stdin.write('\n');
            }
            IO.copy(plaindata,stdin);
        }

        // Read encrypted output
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IO.copy(process.getInputStream(), output);

        // Capture errors
        StringBuilder errorOutput = new StringBuilder();
        try (InputStream stderr = process.getErrorStream())
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = stderr.read(buffer)) != -1)
            {
                errorOutput.append(new String(buffer, 0, bytesRead));
            }
        }

        // Check result
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new IOException("Error al cifrar/firmar con GPG. Código: " + exitCode
                    + "\nError: " + errorOutput);
        }

        return output.toByteArray();
    }

    /**
     * Carries the metadata produced by a
     * {@link GPG#decryptAndVerify decryptAndVerify} operation.
     *
     * <p>An instance of this class can be passed to
     * {@link GPG#decryptAndVerify(byte[], char[], DecryptStatus)} so that
     * the caller can inspect signature validity, signer identity, and the
     * list of key IDs for which the message was encrypted, in addition to
     * receiving the decrypted payload.</p>
     */
    public static class DecryptStatus
    {
        volatile String signer;
        volatile boolean validSignature;
        volatile boolean decryptionOkay;
        volatile List<String> recipients = new ArrayList<>();
        volatile String comment;
        volatile String hash;
        volatile String version;

        /**
         * Returns the key ID of the signer, or {@code null} if the message
         * was not signed or the signer could not be determined.
         *
         * @return signer key ID, or {@code null}
         */
        public String getSigner()
        {
            return signer;
        }

        /**
         * Returns {@code true} if GPG reported a valid ({@code GOODSIG})
         * signature for this message.
         *
         * @return {@code true} if the signature is valid
         */
        public boolean isValidSignature()
        {
            return validSignature;
        }

        /**
         * Returns {@code true} if GPG reported successful decryption
         * ({@code DECRYPTION_OKAY}) for this message.
         *
         * @return {@code true} if decryption succeeded
         */
        public boolean isDecryptionOkay()
        {
            return decryptionOkay;
        }

        /**
         * Returns the key IDs of all recipients for whom the message was
         * encrypted, as reported by GPG {@code ENC_TO} status lines.
         *
         * @return array of recipient key IDs; never {@code null}, may be empty
         */
        public String[] getRecipients()
        {
            return recipients.toArray(new String[0]);
        }

        /**
         * Returns the {@code Comment} field from the ASCII-armor header, or
         * {@code null} if the message was not armored or the header was absent.
         *
         * @return armor comment, or {@code null}
         */
        public String getComment()
        {
            return comment;
        }

        /**
         * Returns the {@code Hash} field from the ASCII-armor header, or
         * {@code null} if not present.
         *
         * @return armor hash algorithm name, or {@code null}
         */
        public String getHash()
        {
            return hash;
        }

        /**
         * Returns the {@code Version} field from the ASCII-armor header, or
         * {@code null} if not present.
         *
         * @return GPG version string embedded in the armor, or {@code null}
         */
        public String getVersion()
        {
            return version;
        }
    }
    /**
     * Decrypts and verifies the signature of a byte array encrypted with GPG.
     *
     * @param cipherdata Encrypted (and possibly signed) byte array.
     * @param passphrase Passphrase for the private key (or null to not use it).
     * @return Map with: "decrypted" (decrypted byte[], "signer" (signer ID or
     * null), "signatureValid" (Boolean, true if the signature is valid, false
     * if there is no signature or it is invalid), "recipients" (List<String>
     * with recipient IDs/fingerprints).
     * @throws IOException If GPG or I/O fails.
     * @throws InterruptedException If the GPG process is interrupted.
     */
    public byte[] decryptAndVerify(byte[] cipherdata, char[] passphrase, DecryptStatus status) throws IOException, InterruptedException
    {
        Objects.requireNonNull(cipherdata, "cipherdata must not be null");
        return decryptAndVerify(new ByteArrayInputStream(cipherdata), passphrase, status);
    }
    /**
     * Decrypts and verifies the signature of data read from an
     * {@link InputStream} encrypted with GPG.
     *
     * <p>GPG status lines are parsed to populate the supplied
     * {@link DecryptStatus} object (if non-{@code null}).  If {@code status}
     * is {@code null} an internal instance is used.</p>
     *
     * @param cipherdata stream providing the encrypted (and optionally signed)
     *                   data; must not be {@code null}
     * @param passphrase passphrase for the private key, or {@code null} if the
     *                   key has no passphrase
     * @param status     optional object to receive signature/decryption metadata;
     *                   may be {@code null}
     * @return decrypted byte array if decryption succeeded and the signature
     *         (when present) is valid; {@code null} otherwise
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted
     */
    public byte[] decryptAndVerify(InputStream cipherdata, char[] passphrase, DecryptStatus status) throws IOException, InterruptedException
    {
        Objects.requireNonNull(cipherdata, "cipherdata must not be null");
        boolean pass = passphrase != null && passphrase.length!=0;
        GnuPG gnupg = gpg(BATCH, NOTTY, VERBOSE, "--decrypt", OUTPUT, "-", STATUS_FD_2);
        if (pass)
        {
            gnupg.add(PASSPHRASE_FD_0, PINENTRYMODE_LOOPBACK);
        }

        Process process = gnupg.start();

        // send passphrase when apply and data
        try (OutputStream stdin = process.getOutputStream())
        {
            if (pass)
            {
                stdin.write(Byter.bytesUTF8(passphrase));
                stdin.write('\n');
            }
            IO.copy(cipherdata,stdin);
        }

        // Read decrypted output and status
        ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();
        IO.copy(process.getInputStream(), decryptedOutput);
        
        if(status==null)
        {
            status = new DecryptStatus();
        }
        else
        {
            status.recipients.clear();
        }
        
        try (BufferedReader stderr = debugger(new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))))
        {
            String line;
            Matcher matcher;
            while((line=stderr.readLine())!=null)
            {
                String tuc = line.trim().toUpperCase();
                
                if (tuc.startsWith("[GNUPG:] GOODSIG"))
                {
                    status.validSignature = true;
                    String[] parts = line.split(" ");
                    if (parts.length >= 3)
                    {
                        status.signer = parts[2];
                    }
                }
                else if (tuc.startsWith("[GNUPG:] BADSIG"))
                {
                    status.validSignature = false;
                    String[] parts = line.split(" ");
                    if (parts.length >= 3)
                    {
                        status.signer = parts[2];
                    }
                }
                else if (tuc.startsWith("[GNUPG:] ENC_TO"))
                {
                    String[] parts = line.split(" ");
                    if (parts.length >= 3)
                    {
                        status.recipients.add(parts[2]);
                    }
                }
                else if (tuc.contains("DECRYPTION_OKAY"))
                {
                    status.decryptionOkay=true;
                }
                else if ((matcher = GPG_ARMOR_HEADER_PATTERN.matcher(line)).matches())
                {
                    String header = matcher.group(1).toLowerCase();
                    String value = matcher.group(2);
                    if (header.equals("comment"))
                    {
                        status.comment = value;
                    }
                    else if (header.equals("version"))
                    {
                        status.version = value;
                    }
                    else if (header.equals("hash"))
                    {
                        status.hash = value;
                    }
                }
                else if(debug)
                {
                    System.err.println(line);
                }
            }
        }

        // Check result
        int exitCode = process.waitFor();
        if (exitCode != 0 && (!status.validSignature || !status.decryptionOkay))
        {
            return null;
        }
        
        return status.validSignature ? decryptedOutput.toByteArray() : null;
    }

    /**
     * Gets the IDs or fingerprints of the keys for which a message is
     * encrypted.
     *
     * @param cipherdata Array of encrypted bytes.
     * @param passphrase Passphrase to access the message (or null to not use it).
     * @return List of IDs/fingerprints of the recipients.
     * @throws IOException If GPG or I/O fails.
     * @throws InterruptedException If the GPG process is interrupted.
     *
     */
    public String[] getEncryptionRecipients(byte[] cipherdata, char[] passphrase) throws IOException, InterruptedException
    {
        Objects.requireNonNull(cipherdata, "cipherdata must not be null");
        if (cipherdata.length == 0)
        {
            throw new IllegalArgumentException("cipherdata must not be empty");
        }
        return getEncryptionRecipients(new ByteArrayInputStream(cipherdata), passphrase);
    }
    /**
     * Extracts the recipient key IDs from an encrypted GPG message provided
     * as an {@link InputStream}.
     *
     * <p>Uses {@code gpg --list-packets} to inspect the message without fully
     * decrypting it.</p>
     *
     * @param cipherdata stream providing the encrypted data; must not be
     *                   {@code null}
     * @param passphrase passphrase to supply to GPG when needed (e.g. for
     *                   symmetric encryption), or {@code null}
     * @return array of recipient key IDs found in the message; never
     *         {@code null}, may be empty
     * @throws IOException          if the GPG process cannot be started, an I/O
     *                              error occurs, or GPG returns a non-zero exit code
     * @throws InterruptedException if the calling thread is interrupted
     */
    public String[] getEncryptionRecipients(InputStream cipherdata, char[] passphrase) throws IOException, InterruptedException
    {
        boolean pass = passphrase != null && passphrase.length!=0;
        // Build GPG command
        GnuPG gnupg = gpg(BATCH, NOTTY, "--list-packets");
        if (pass)
        {
            gnupg.add(PASSPHRASE_FD_0);
        }

        Process process = gnupg.start();

        // Send passphrase (if applicable) and details
        try (OutputStream stdin = process.getOutputStream())
        {
            if (pass)
            {
                stdin.write(Byter.bytesUTF8(passphrase));
                stdin.write('\n');
            }
            IO.copy(cipherdata,stdin);
        }

        // Read output
        StringBuilder output = new StringBuilder();
        try (InputStream stdout = process.getInputStream())
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = stdout.read(buffer)) != -1)
            {
                output.append(new String(buffer, 0, bytesRead));
            }
        }

        // Check result
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new IOException("Error listing packages with GPG. Code: " + exitCode
                    + "\nError: " + output);
        }

        // Extract receptors
        HashSet<String> recipients = new HashSet<>();
        String[] lines = output.toString().split("\n");
        for (String line : lines)
        {
            if(line.startsWith(":pubkey") && line.contains("keyid"))
            {
                String[] parts = line.trim().split("keyid ");
                if (parts.length > 1)
                {
                    String keyId = parts[1].trim();
                    recipients.add(keyId);
                }
            }
        }

        return recipients.toArray(new String[0]);
    }
    
    
    /**
     * Represents the parsed metadata of a single OpenPGP packet as produced
     * by {@code gpg --list-packets}.
     *
     * <p>The fields map directly to the values reported on the packet header
     * line (offset, CTB, tag, header length, packet length) and on the packet
     * body line (version, algorithm, key ID).</p>
     */
    public static class PacketInfo
    {
        /** Byte offset of the packet within the data stream. */
        public final int off;

        /** Cipher Tag Byte (CTB) — encodes the packet tag and length format. */
        public final int ctb;

        /** OpenPGP packet tag number (e.g. {@code 1} = public-key encrypted
         *  session key, {@code 2} = signature, {@code 9} = symmetrically
         *  encrypted data). */
        public final int tag;

        /** Length of the packet header in bytes. */
        public final int hlen;

        /** Length of the packet body (payload) in bytes. */
        public final int plen;

        /** Packet format version (typically {@code 3} or {@code 4}). */
        public final int version;

        /** Public-key algorithm identifier used in this packet. */
        public final int algorithm;

        /** Short key ID of the key referenced by this packet, or {@code null}
         *  when not applicable. */
        public final String keyId;

        /**
         * Constructs a {@code PacketInfo} with all fields explicitly supplied.
         *
         * @param off       byte offset of the packet
         * @param ctb       cipher tag byte
         * @param tag       OpenPGP packet tag
         * @param hlen      header length in bytes
         * @param plen      payload length in bytes
         * @param version   packet version
         * @param algorithm public-key algorithm identifier
         * @param keyId     short key ID, or {@code null}
         */
        public PacketInfo(int off, int ctb, int tag, int hlen, int plen, int version, int algorithm, String keyId)
        {
            this.off = off;
            this.ctb = ctb;
            this.tag = tag;
            this.hlen = hlen;
            this.plen = plen;
            this.version = version;
            this.algorithm = algorithm;
            this.keyId = keyId;
        }

        /**
         * Returns a human-readable summary of all fields in this packet record.
         *
         * @return string representation for debugging
         */
        @Override
        public String toString()
        {
            return "off=" + off + ", ctb=" + ctb + ", tag=" + tag + ", hlen=" + hlen + ", plen=" + plen + ", version=" + version + ", algorithm=" + algorithm + ", keyId=" + keyId;
        }
    }
    
    /**
     * Inspects an encrypted byte array and returns high-level metadata about
     * its OpenPGP packets without fully decrypting the payload.
     *
     * @param cipherdata  the encrypted data to inspect; must not be {@code null}
     * @param passphrase  passphrase for symmetric decryption, or {@code null}
     * @return a {@link PacketsInfo} object populated with the parsed metadata
     * @throws IOException          if the GPG process cannot be started or an
     *                              I/O error occurs
     * @throws InterruptedException if the calling thread is interrupted
     */
    public PacketsInfo listPackets(byte[] cipherdata, char[] passphrase) throws IOException, InterruptedException
    {
        return listPackets(new ByteArrayInputStream(cipherdata), passphrase);
    }

    /**
     * Aggregates the high-level status information extracted from an encrypted
     * GPG message by the {@link GPG#listPackets listPackets} operation.
     *
     * <p>The fields are populated from GPG {@code [GNUPG:]} status lines and,
     * when the message is ASCII-armored, from the armor header fields.</p>
     */
    public static class PacketsInfo
    {
        /** Short key ID of the recipient key reported in the {@code ENC_TO}
         *  status line, or {@code null} if not found. */
        public final String encTo;

        /** Algorithm number (as a string) from the {@code ENC_TO} status line,
         *  or {@code null} if not found. */
        public final String algo;

        /** Fingerprint of the subkey used for decryption, from the
         *  {@code DECRYPTION_KEY} status line, or {@code null}. */
        public final String subKey;

        /** Fingerprint of the primary key that owns the decryption subkey,
         *  from the {@code DECRYPTION_KEY} status line, or {@code null}. */
        public final String mainKey;

        /** Trust level of the decryption key as reported by GPG, or
         *  {@code null}. */
        public final String trust;

        /** {@code true} if GPG reported {@code DECRYPTION_OKAY}. */
        public final boolean decryptionOkay;

        /** {@code true} if GPG reported {@code GOODMDC} (Modification
         *  Detection Code verified). */
        public final boolean goodmdc;

        /** {@code Comment} field from the ASCII-armor header, or {@code null}. */
        public final String comment;

        /** {@code Hash} field from the ASCII-armor header, or {@code null}. */
        public final String hash;

        /** {@code Version} field from the ASCII-armor header, or {@code null}. */
        public final String version;

        /**
         * Constructs a {@code PacketsInfo} with all fields explicitly supplied.
         *
         * @param encTo          recipient key ID from {@code ENC_TO}, or {@code null}
         * @param algo           algorithm string from {@code ENC_TO}, or {@code null}
         * @param subKey         decryption subkey fingerprint, or {@code null}
         * @param mainKey        primary key fingerprint, or {@code null}
         * @param trust          key trust level, or {@code null}
         * @param decryptionOkay {@code true} if decryption succeeded
         * @param goodmdc        {@code true} if MDC check passed
         * @param comment        armor comment header value, or {@code null}
         * @param hash           armor hash header value, or {@code null}
         * @param version        armor version header value, or {@code null}
         */
        public PacketsInfo(String encTo, String algo, String subKey, String mainKey, String trust, boolean decryptionOkay, boolean goodmdc, String comment, String hash, String version)
        {
            this.encTo = encTo;
            this.algo = algo;
            this.subKey = subKey;
            this.mainKey = mainKey;
            this.trust = trust;
            this.decryptionOkay = decryptionOkay;
            this.goodmdc = goodmdc;
            this.comment = comment;
            this.hash = hash;
            this.version = version;
        }

        /**
         * Returns a human-readable summary of all fields in this
         * {@code PacketsInfo} record.
         *
         * @return string representation for debugging
         */
        @Override
        public String toString()
        {
            return "encTo=" + encTo + ", algo=" + algo + ", subKey=" + subKey + ", mainKey=" + mainKey + ", trust=" + trust + ", decryptionOkay=" + decryptionOkay + ", goodmdc=" + goodmdc + ", comment=" + comment+ ", hash=" + hash + ", version=" + version;
        }
    }
    /**
     * Inspects an encrypted GPG stream and returns high-level metadata about
     * its OpenPGP packets without fully decrypting the payload.
     *
     * <p>Runs {@code gpg --batch --no-tty --verbose --list-packets
     * --status-fd=1} and parses the resulting {@code [GNUPG:]} status lines
     * along with any ASCII-armor headers.</p>
     *
     * @param cipherdata stream providing the encrypted data; must not be
     *                   {@code null}
     * @param passphrase passphrase for symmetric decryption, or {@code null}
     * @return a {@link PacketsInfo} object populated with the parsed metadata;
     *         fields for which no information was found will be {@code null}
     * @throws IOException          if the GPG process cannot be started, an I/O
     *                              error occurs, or GPG exits with an unexpected
     *                              non-zero code (codes 0 and 2 are accepted)
     * @throws InterruptedException if the calling thread is interrupted
     */
    public PacketsInfo listPackets(InputStream cipherdata, char[] passphrase) throws IOException, InterruptedException
    {
        boolean pass = passphrase != null && passphrase.length!=0;
        // Build GPG command
        GnuPG gnupg = gpg(BATCH, NOTTY, VERBOSE, "--list-packets", STATUS_FD_1).merge();

        if (pass)
        {
            gnupg.add(PASSPHRASE_FD_0);
        }
        else
        {
            gnupg.add(PINENTRYMODE_CANCEL);
        }
        
        gnupg.add("--");

        Process process = gnupg.start();

        // Send passphrase (if applicable) and details
        try (OutputStream stdin = process.getOutputStream())
        {
            if (pass)
            {
                stdin.write(Byter.bytesUTF8(passphrase));
                stdin.write('\n');
            }
            IO.copy(cipherdata, stdin);
        }

        String encTo=null;
        String algo=null;
        String subKey=null;
        String mainKey=null;
        String trust=null;
        boolean decryptionOkay=false;
        boolean goodmdc=false;
        String headerComment=null;
        String headerHash=null;
        String headerVersion=null;

        try (BufferedReader stdout = debugger(new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))))
        {
            String line;
            Matcher matcher;
            while((line=stdout.readLine())!=null)
            {
                String[] s = line.trim().toUpperCase().split(" ");
                String cmd = s[1];
                
                if(cmd.equals("ENC_TO"))
                {
                    //[GNUPG:] ENC_TO F158E66C4BBDC041 18 0
                    encTo = s[2];
                    algo = s[3];
                }
                else if(cmd.equals("DECRYPTION_KEY"))
                {
                    subKey = s[2];
                    mainKey = s[3];
                    trust = s[4];
                }
                else if(cmd.equals("DECRYPTION_OKAY"))
                {
                    decryptionOkay=true;
                }
                else if(cmd.equals("GOODMDC"))
                {
                    goodmdc=true;
                }
                else if((matcher=GPG_ARMOR_HEADER_PATTERN.matcher(line)).matches())
                {
                    String header = matcher.group(1).toLowerCase();
                    String value = matcher.group(2);
                    if (header.equals("comment"))
                    {
                        headerComment = value;
                    }
                    else if (header.equals("version"))
                    {
                        headerVersion = value;
                    }
                    else if (header.equals("hash"))
                    {
                        headerHash = value;
                    }
                }
                else if(debug)
                {
                    System.err.println(line);
                }
            }
        }
        // Check result
        int exitCode = process.waitFor();
        if (exitCode != 0 && exitCode!=2)
        {
            throw new IOException("Error listing packages with GPG. Code: " + exitCode);
        }
        
        return new PacketsInfo(encTo, algo, subKey, mainKey, trust, decryptionOkay, goodmdc, headerComment, headerHash, headerVersion);
    }

    /**
     * Sends public keys to the default keyserver.
     *
     * @param keyIds Array of key IDs (fingerprint, short ID, or email) to send
     * @return 0 on success or GPG error codes on error
     * @throws IOException If GPG or I/O fails
     * @throws InterruptedException If the GPG process is interrupted
     */
    public int sendKeys(String... keyIds) throws IOException, InterruptedException
    {
        Objects.requireNonNull(keyIds, "keyIds must not be null");
        if (keyIds.length == 0)
        {
            throw new IllegalArgumentException("keyIds cannot be empty");
        }

        GnuPG gnupg = gpg(BATCH, "--send-keys");
        gnupg.add(keyIds);

        Process process = gnupg.start();

        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            try (Scanner sc = new Scanner(process.getInputStream()))
            {
                while (sc.hasNext())
                {
                    System.err.println(sc.nextLine());
                }
            }
        }
        return exitCode;
    }
    /**
     * Receives public keys from the default keyserver.
     *
     * @param keyIds Array of key IDs (fingerprint, short ID, or email) to
     * receive
     * @return 0 on success or GPG error codes on error
     * @throws IOException If GPG or I/O fails
     * @throws InterruptedException If the GPG process is interrupted
     */
    public int receiveKeys(String... keyIds) throws IOException, InterruptedException
    {
        Objects.requireNonNull(keyIds, "keyIds must not be null");
        if (keyIds.length == 0)
        {
            throw new IllegalArgumentException("keyIds cannot be empty");
        }

        GnuPG gnupg = gpg(BATCH, "--receive-keys");
        gnupg.add(keyIds);

        Process process = gnupg.start();

        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            try (Scanner sc = new Scanner(process.getInputStream()))
            {
                while (sc.hasNext())
                {
                    System.err.println(sc.nextLine());
                }
            }
        }
        return exitCode;
    }
    /**
     * Refreshes all keys from the default keyserver.
     * 
     * @return 0 on success or GPG error codes on error
     * @throws IOException If GPG or I/O fails
     * @throws InterruptedException If the GPG process is interrupted
     */
    public int refreshKeys() throws IOException, InterruptedException
    {
        Process process = gpg(BATCH, "--refresh-keys").start();

        int exitCode = process.waitFor();
        if (exitCode != 0) 
        {
            try (Scanner sc = new Scanner(process.getInputStream())) 
            {
                while(sc.hasNext())
                {
                    System.err.println(sc.nextLine());
                }
            }
        }
        return exitCode;
    }    
}
