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
 * A utility class for interacting with the Snapcraft (Snap) runtime
 * environment.
 * <p>
 * This class provides access to standard Snap environment variables and offers
 * helper methods to handle common integration challenges when running a Java
 * application inside a Snap package. Snap's sandboxing restricts filesystem
 * access, and this class helps adapt the application's behavior accordingly.
 * <p>
 * This is a non-instantiable utility class.
 *
 * @see <a href="https://snapcraft.io/docs/environment-variables">Snap
 * Environment Variables Documentation</a>
 */
public final class Snap
{

    /**
     * The private constructor to prevent instantiation of this utility class.
     */
    private Snap()
    {
        // This class should not be instantiated.
    }

    /**
     * The mount point of the snap's package, which is a read-only directory.
     * Corresponds to the {@code SNAP} environment variable.
     *
     * @see <a href="https://snapcraft.io/docs/environment-variables">Snap
     * Environment Variables</a>
     */
    public static final String SNAP = System.getenv("SNAP");

    /**
     * The path to the snap's versioned, writable data directory for this
     * specific revision. Corresponds to the {@code SNAP_DATA} environment
     * variable.
     */
    public static final String SNAP_DATA = System.getenv("SNAP_DATA");

    /**
     * The path to the snap's version-independent, writable data directory.
     * Corresponds to the {@code SNAP_COMMON} environment variable.
     */
    public static final String SNAP_COMMON = System.getenv("SNAP_COMMON");

    /**
     * The path to the user's versioned, writable data directory for this snap
     * revision. Corresponds to the {@code SNAP_USER_DATA} environment variable.
     */
    public static final String SNAP_USER_DATA = System.getenv("SNAP_USER_DATA");

    /**
     * The path to the user's version-independent, writable data directory for
     * this snap. Corresponds to the {@code SNAP_USER_COMMON} environment
     * variable.
     */
    public static final String SNAP_USER_COMMON = System.getenv("SNAP_USER_COMMON");

    /**
     * The name of the snap as specified in its {@code snap.yaml}. Corresponds
     * to the {@code SNAP_NAME} environment variable.
     */
    public static final String SNAP_NAME = System.getenv("SNAP_NAME");

    /**
     * The version string of the snap. Corresponds to the {@code SNAP_VERSION}
     * environment variable.
     */
    public static final String SNAP_VERSION = System.getenv("SNAP_VERSION");

    /**
     * The revision of the snap, which is an integer that increments with each
     * upload. Corresponds to the {@code SNAP_REVISION} environment variable.
     */
    public static final String SNAP_REVISION = System.getenv("SNAP_REVISION");

    /**
     * The architecture of the host system (e.g., amd64, arm64). Corresponds to
     * the {@code SNAP_ARCH} environment variable.
     */
    public static final String SNAP_ARCH = System.getenv("SNAP_ARCH");

    /**
     * The instance name of the snap, which includes the instance key if
     * specified. Corresponds to the {@code SNAP_INSTANCE_NAME} environment
     * variable.
     */
    public static final String SNAP_INSTANCE_NAME = System.getenv("SNAP_INSTANCE_NAME");
    
    /**
     * Checks whether the application is currently running inside a Snap
     * container. This is determined by checking for the presence of the
     * {@code SNAP} environment variable.
     *
     * @return {@code true} if the {@code SNAP} environment variable is set and
     * not empty, {@code false} otherwise.
     */
    public static boolean isSnap()
    {
        return SNAP!=null && !SNAP.isEmpty();
    }

    /**
     * Redirects the Java temporary directory to a writable location within the
     * snap's user data area.
     * <p>
     * Snap's sandboxing often prevents writing to the system's default
     * temporary directory (e.g., {@code /tmp}). This method provides a
     * workaround by creating a "tmp" subdirectory inside
     * {@code $SNAP_USER_COMMON} or {@code $SNAP_USER_DATA} and setting the
     * {@code java.io.tmpdir} and {@code jna.tmpdir} system properties to point
     * to it.
     * <p>
     * It prefers {@code SNAP_USER_COMMON} but falls back to
     * {@code SNAP_USER_DATA}. If the directory does not exist, it will be
     * created. If the application is not running in a Snap environment
     * (detected by the absence of the required environment variables), this
     * method does nothing and returns {@code null}.
     *
     * @return A {@link File} object representing the new temporary directory,
     * or {@code null} if the application is not running in a Snap environment.
     */
    public static File fixTmpDir()
    {
        String base = SNAP_USER_COMMON != null ? SNAP_USER_COMMON : SNAP_USER_DATA;
        if (base == null)
        {
            return null; // Not running inside Snap
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
        return tmpDir;
    }    
}
