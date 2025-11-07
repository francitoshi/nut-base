/*
 *  GPG.java
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

import io.nut.base.io.IO;
import io.nut.base.io.VerboseLineReader;
import io.nut.base.util.Args;
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
import java.util.Scanner;
import java.util.StringJoiner;

//https://github.com/gpg/gnupg/blob/master/doc/DETAILS

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

    // OTHER
    
    private static final int BUFFER_SIZE = 8*1024;
    
    private volatile boolean debug;
    private volatile boolean armor;

    private BufferedReader debugger(BufferedReader src)
    {
        return this.debug ? new VerboseLineReader(src, System.out) : src;
    }
    
    private static class GnuPG
    {
        final boolean debug;
        final Args args = new Args();
        public GnuPG(boolean debug, String... params)
        {
            this.debug = debug;
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
        public Process start() throws IOException
        {
            final ProcessBuilder pb = new ProcessBuilder("gpg");
            pb.command().addAll(args.get());
            pb.redirectErrorStream(false);

            if(debug)
            {
                StringJoiner sb = new StringJoiner(" ");
                for(String item : pb.command())
                {
                    sb.add(item);
                }
                System.out.println(sb.toString());
            }
            return pb.start();
        }

        @Override
        public String toString()
        {
            StringJoiner sj = new StringJoiner(" ");
            sj.add("gpg");
            for(String item : args.get())
            {
                sj.add(item);
            }
            return sj.toString();
        }
        
    }

    private static final String PINENTRYMODE_CANCEL = "--pinentry-mode=cancel";
    private static final String PINENTRYMODE_LOOPBACK = "--pinentry-mode=loopback";
    private static final String PASSPHRASE_FD_0 = "--passphrase-fd=0";
    private static final String STATUS_FD_1 = "--status-fd=1";
    private static final String STATUS_FD_2 = "--status-fd=2";
    
    public GPG setDebug(boolean value)
    {
        this.debug = value;
        return this;
    }
    public GPG setArmor(boolean value)
    {
        this.armor = value;
        return this;
    }

    private GnuPG gpg(String... params) throws IOException 
    {
        return new GnuPG(debug, params);
    }
    
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
    
    public PubKey[] getPubKeys(String... lookfor) throws IOException, InterruptedException
    {
        List<PubKey> pubs = new ArrayList<>();
        Process process = gpg(BATCH, "--list-public-keys","--with-colons").add(lookfor).start();
        parseKeys(process.getInputStream(), pubs, null);
        process.waitFor();
        return pubs.toArray(new PubKey[0]);
    }
    
    public SecKey[] getSecKeys(String... lookfor) throws IOException, InterruptedException
    {
        List<SecKey> secs = new ArrayList<>();
        Process process = gpg(BATCH, "--list-secret-keys","--with-colons").add(lookfor).start();
        parseKeys(process.getInputStream(), null, secs);
        process.waitFor();
        return secs.toArray(new SecKey[0]);
    }

    public int deletePubKeys(String name) throws IOException, InterruptedException
    {
        return gpg(BATCH, YES,"--delete-keys",name).start().waitFor();
    }
    
    public int deleteSecKeys(String fingerprint) throws IOException, InterruptedException
    {
        return gpg(BATCH, YES,"--delete-secret-keys", fingerprint).start().waitFor();
    }
    
    public int deleteSecAndPubKeys(String fingerprint) throws IOException, InterruptedException
    {
        return gpg(BATCH, YES,"--delete-secret-and-public-key", fingerprint).start().waitFor();
    }
    
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
    
    public int addKeyRSA(String keyId, int ssbBits, boolean sign, boolean encrypt, boolean auth, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, true, false, false, false, ssbBits, sign, encrypt, auth,  0, expire, passphrase);
    }
    public int addKeyDSA(String keyId, int ssbBits, boolean sign, boolean auth, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, false, true, false, false, ssbBits, sign, false, auth,  0, expire, passphrase);
    }
    public int addKeyELG(String keyId, int ssbBits, String expire, String passphrase) throws IOException, InterruptedException
    {
        return editKeyAddKey(keyId, false, false, true, false, ssbBits, false, true, false,  0, expire, passphrase);
    }
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
            }
            else if(dsa)
            {
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
            }
            else if(elg)
            {
                ops = "5";
            }
            else if(ecc)
            {
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
        
        GnuPG gnupg = gpg(BATCH, NOTTY).add(armor, ARMOR).add("--encrypt", "--output", "-");
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

    public static class DecryptStatus
    {
        volatile byte[] plaintext;
        volatile String signer;
        volatile boolean validSignature;
        volatile String[] recipients;
        volatile String errorText;        
        volatile String statusText;        
        public byte[] getPlaintext()
        {
            return plaintext;
        }

        public String getSigner()
        {
            return signer;
        }

        public boolean isValidSignature()
        {
            return validSignature;
        }

        public String[] getRecipients()
        {
            return recipients;
        }

        public String getErrorText()
        {
            return errorText;
        }

        public String getStatusText()
        {
            return statusText;
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
        if (cipherdata==null)
        {
            throw new NullPointerException("encryptedData is null");
        }
        return decryptAndVerify(new ByteArrayInputStream(cipherdata), passphrase, status);
    }
    public byte[] decryptAndVerify(InputStream cipherdata, char[] passphrase, DecryptStatus status) throws IOException, InterruptedException
    {
        boolean pass = passphrase != null && passphrase.length!=0;
        GnuPG gnupg = gpg(BATCH, NOTTY, "--decrypt", "--output", "-", STATUS_FD_2);
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
        String errorString = errorOutput.toString();
        if (exitCode != 0 && !errorString.contains("DECRYPTION_OKAY"))
        {
            throw new IOException("Error al desencriptar/verificar con GPG. Código: " + exitCode
                    + "\nError: " + errorString);
        }

        // Procesar estado
        String signer = null;
        boolean validSignature = false;
        List<String> recipients = new ArrayList<>();

        StringBuilder errorText = new StringBuilder();
        StringBuilder statusText = new StringBuilder();

        String[] statusLines = errorString.split("\n");
        for (String line : statusLines)
        {
            if (line.startsWith("[GNUPG:] GOODSIG"))
            {
                validSignature = true;
                String[] parts = line.split(" ");
                if (parts.length >= 3)
                {
                    signer = parts[2];
                }
                statusText.append(line).append('\n');
            }
            else if (line.startsWith("[GNUPG:] BADSIG"))
            {
                validSignature = false;
                String[] parts = line.split(" ");
                if (parts.length >= 3)
                {
                    signer = parts[2];
                }
                statusText.append(line).append('\n');
            }
            else if (line.startsWith("[GNUPG:] ENC_TO"))
            {
                String[] parts = line.split(" ");
                if (parts.length >= 3)
                {
                    recipients.add(parts[2]);
                }
                statusText.append(line).append('\n');
            }
            else if (line.startsWith("[GNUPG:]"))
            {
                statusText.append(line).append('\n');
            }
            else
            {
                errorText.append(line).append('\n');
            }
        }
        byte[] plaintext = validSignature ? decryptedOutput.toByteArray() : null;
        if(status!=null)
        {
            status.plaintext = plaintext;
            status.signer = signer;
            status.validSignature = validSignature;
            status.recipients = recipients.toArray(new String[0]);
            status.errorText = errorText.toString();
            status.statusText = statusText.toString();
        }
        return plaintext;
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
        if (cipherdata == null || cipherdata.length == 0)
        {
            throw new IllegalArgumentException("Los datos cifrados no pueden ser nulos o vacíos.");
        }
        return getEncryptionRecipients(new ByteArrayInputStream(cipherdata), passphrase);
    }
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
    
    
    public static class PacketInfo
    {
        public final int off;
        public final int ctb;
        public final int tag;
        public final int hlen;
        public final int plen;

        public final int version;
        public final int algorithm;
        public final String keyId;

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

        @Override
        public String toString()
        {
            return "off=" + off + ", ctb=" + ctb + ", tag=" + tag + ", hlen=" + hlen + ", plen=" + plen + ", version=" + version + ", algorithm=" + algorithm + ", keyId=" + keyId;
        }
    }
    
    public PacketsInfo listPackets(byte[] cipherdata, char[] passphrase) throws IOException, InterruptedException
    {
        return listPackets(new ByteArrayInputStream(cipherdata), passphrase);
    }

    public static class PacketsInfo
    {
        public final String encTo;
        public final String algo;
        public final String subKey;
        public final String mainKey;
        public final String trust;
        public final boolean decryptionOkay;
        public final boolean goodmdc;
        public PacketsInfo(String encTo, String algo, String subKey, String mainKey, String trust, boolean decryptionOkay, boolean goodmdc)
        {
            this.encTo = encTo;
            this.algo = algo;
            this.subKey = subKey;
            this.mainKey = mainKey;
            this.trust = trust;
            this.decryptionOkay = decryptionOkay;
            this.goodmdc = goodmdc;
        }

        @Override
        public String toString()
        {
            return "encTo=" + encTo + ", algo=" + algo + ", subKey=" + subKey + ", mainKey=" + mainKey + ", trust=" + trust + ", decryptionOkay=" + decryptionOkay + ", goodmdc=" + goodmdc;
        }        
    }
    
    public PacketsInfo listPackets(InputStream cipherdata, char[] passphrase) throws IOException, InterruptedException
    {
        boolean pass = passphrase != null && passphrase.length!=0;
        // Build GPG command
        GnuPG gnupg = gpg(BATCH, NOTTY, "--list-packets", STATUS_FD_1);

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

        try (BufferedReader stdout = debugger(new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))))
        {
            String line;

            while((line=stdout.readLine())!=null)
            {
                String[] s = line.trim().toUpperCase().split(" ");
                String cmd = s[1].toUpperCase();
                
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
        
        return new PacketsInfo(encTo, algo, subKey, mainKey, trust, decryptionOkay, goodmdc);
    }
    
}
