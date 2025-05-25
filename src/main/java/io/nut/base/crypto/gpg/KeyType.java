package io.nut.base.crypto.gpg;

/**
 *
 * @author franci
 */
public class KeyType
{
    public static final String ESCA = GPG.ESCA;
    public static final String E    = GPG.E;
    public static final String SCA  = GPG.SCA;
    
    public final String id;
    public final String type;
    public final int bits;
    public final String curve;
    public final String usage;
    public final boolean encrypt;
    public final boolean sign;
    public final boolean cert;
    public final boolean auth;
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

    @Override
    public String toString()
    {
        return id+(encrypt?"E":"")+(sign?"S":"")+(cert?"C":"")+(auth?"A":"");
    }
    
}
