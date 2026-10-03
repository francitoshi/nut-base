/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//2018-07-11 francitoshi 
//• copied from https://github.com/codahale/shamir
//• remove use of Google AutoValue 
//• drop use of lambdas

package io.nut.base.crypto.shamir;

import io.nut.base.crypto.Kripto;
import java.security.SecureRandom;

/**
 * {@link ShamirScheme} implemented Shamir's Secret Sharing over {@code GF(256)} to securely split secrets
 * into {@code N} parts, of which any {@code K} can be joined to recover the original secret.
 * <p>
 * {@link ShamirScheme} uses the same GF(256) field polynomial as the Advanced Encryption Standard (AES):
 * {@code 0x11b}, or {@code x}<sup>8</sup> + {@code x}<sup>4</sup> + {@code x}<sup>3</sup> +
 * {@code x} + 1.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing">Shamir's Secret
 * Sharing</a>
 * @see <a href="http://www.cs.utsa.edu/~wagner/laws/FFM.html">The Finite Field {@code GF(256)}</a>
 */

public class ShamirScheme 
{
    private final SecureRandom secureRandom;

    final int n;
    final int k;

    private ShamirScheme(int n, int k)
    {
        this.n = n;
        this.k = k;
        this.secureRandom = Kripto.getSecureRandomStrong();
    }
  
  /**
   * Creates a new {@link ShamirScheme} instance.
   *
   * @param n the number of parts to produce (must be {@code >1})
   * @param k the threshold of joinable parts (must be {@code <= n})
   * @return an {@code N}/{@code K} {@link ShamirScheme}
   */
  
  public static ShamirScheme of(int n, int k) {
    checkArgument(k > 1, "K must be > 1");
    checkArgument(n >= k, "N must be >= K");
    checkArgument(n <= 255, "N must be <= 255");
    return new ShamirScheme(n, k);
  }

  private static void checkArgument(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * The number of parts the scheme will generate when splitting a secret.
   *
   * @return {@code N}
   */
  public int n()
  {
      return this.n;
  }

  /**
   * The number of parts the scheme will require to re-create a secret.
   *
   * @return {@code K}
   */
  public int k()
  {
      return this.k;
  }

    /**
   * Splits the given secret into {@code n} parts, of which any {@code k} or more can be combined
   * to recover the original secret.
   *
   * @param secret the secret to split
   * @return a map of {@code n} part IDs and their values
   */
  
    public byte[][] split(byte[] secret)
    {
        // generate part values
        final byte[][] values = new byte[this.n][secret.length+1];
        for(int i=0;i<values.length;i++)
        {
            values[i][0] = (byte)(i+1);
        }
        for (int i = 0; i < secret.length; i++)
        {
            // for each byte, generate a random polynomial, p
            final byte[] p = GF256.generate(this.secureRandom, this.k-1, secret[i]);
            for (int x = 1; x <= this.n; x++)
            {
                // each part's byte is p(partId)
                values[x - 1][i+1] = GF256.eval(p, (byte) x);
            }
        }
        return values;
    }

  /**
   * Joins the given parts to recover the original secret.
   * <p>
   * <b>N.B.:</b> There is no way to determine whether or not the returned value is actually the
   * original secret. If the parts are incorrect, or are under the threshold value used to split
   * the secret, a random value will be returned.
   *
   * @param parts a map of part IDs to part values
   * @return the original secret
   * @throws IllegalArgumentException if {@code parts} is empty or contains values of varying
   * lengths
   */

    public byte[] join(byte[][] parts)
    {
        checkArgument(parts.length > 0, "No parts provided");
        for(int i=1;i<parts.length;i++)
        {
            checkArgument(parts[i].length == parts[i-1].length, "Varying lengths of part values");
        }
        final byte[] secret = new byte[parts[0].length-1];
        for (int i = 0; i < secret.length; i++)
        {
            final byte[][] points = new byte[parts.length][2];
            
            for(int j=0;j<parts.length;j++)
            {
                points[j][0] = parts[j][0];
                points[j][1] = parts[j][i+1];
            }
            secret[i] = GF256.interpolate(points);
        }
        return secret;
    }

}
























