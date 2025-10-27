/*
 *  Java.java
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
package io.nut.base.platform;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Snap
{
    public static final String SNAP = System.getenv("SNAP");
    public static final String SNAP_DATA = System.getenv("SNAP_DATA");
    public static final String SNAP_COMMON = System.getenv("SNAP_COMMON");
    public static final String SNAP_USER_DATA = System.getenv("SNAP_USER_DATA");
    public static final String SNAP_USER_COMMON = System.getenv("SNAP_USER_COMMON");
    public static final String SNAP_NAME = System.getenv("SNAP_NAME");
    public static final String SNAP_VERSION = System.getenv("SNAP_VERSION");
    public static final String SNAP_REVISION = System.getenv("SNAP_REVISION");
    public static final String SNAP_ARCH = System.getenv("SNAP_ARCH");
    public static final String SNAP_INSTANCE_NAME = System.getenv("SNAP_INSTANCE_NAME");
    
    public static boolean isSnap()
    {
        return SNAP!=null && !SNAP.isEmpty();
    }
    public static void fixTmpDir()
    {
        String base = SNAP_USER_COMMON != null ? SNAP_USER_COMMON : SNAP_USER_DATA;
        if (base == null)
        {
            return; // Not running inside Snap
        }
        File tmpDir = new File(base, "tmp");
        if (!tmpDir.exists())
        {
            tmpDir.mkdirs();
        }

        if (tmpDir.canWrite())
        {
            System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());
            System.setProperty("jna.tmpdir", tmpDir.getAbsolutePath());
        }
        else
        {
            Logger.getLogger(Snap.class.getName()).log(Level.CONFIG, "[SnapFix] Cannot write to tmp dir: {0}", tmpDir.getAbsolutePath());
        }
    }    
}
