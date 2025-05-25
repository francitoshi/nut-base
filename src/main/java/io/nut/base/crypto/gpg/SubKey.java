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
 *
 * @author franci
 */
public class SubKey
{
    public final String type;
    public final String valid;
    public final int bits;
    public final int algorithm;
    public final String keyid;
    public final long createdEpochSecond;
    public final long expiresEpochSecond;
    public final String uid_hash;
    public final char ownertrust;
    public final String sigclass;
    public final String capabilities;
    public final long updateEpochSecond;
    public final String origin;
    private volatile String fingerprint;
    private volatile String grp;

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

    void setFingerprint(String s)
    {
        this.fingerprint = s;
    }

    void setGrp(String s)
    {
        this.grp = s;
    }

    public String getFingerprint()
    {
        return fingerprint;
    }

    public String getGrp()
    {
        return grp;
    }
    
}
