/*
 *  Bip39Test.java
 *
 *  Copyright (C) 2023 francitoshi@gmail.com
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
package io.nut.base.crypto.bips;

import io.nut.base.crypto.bips.Bip39.MnemonicChecksumException;
import io.nut.base.crypto.bips.Bip39.MnemonicWordException;
import io.nut.base.encoding.Hex;
import io.nut.base.util.Strings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class Bip39Test
{

    //test data generated using https://iancoleman.io/bip39/
    static String[] MNEMONICS =
    {
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",//12
        "turkey ripple biology method will stereo speed issue when feed pigeon near quantum minute question",//15
        "sister lobster jealous summer genuine lava oppose arrest coast cement payment hold cargo joke next unit coil sadness",//18
        "surround soul elephant cabin festival start news wisdom venue adult inquiry when lazy purse olympic soap arch mix laugh soldier coyote",//21
        "neglect crucial unknown gaze canal brush idle frown verb flight essence rigid wage drink thunder village chat glance nasty lunar someone simple area nerve"//24
    };
    static String[] PASSPHRASES =
    {
        "",
        "Frodo",
        "Valar Morghulis. Valar Dohaeris."
    };

    static String[][] SEEDS_HEX =
    {
        {
            "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",
            "a3799feef0cc4c21fa8296acf2db6c591a147acfe041f6c42d6bb3e4307b3d95c27c15f0b0e3280d86f2712ca98e83c7e13087973d3ee6a5dbc428f91b509fd9",
            "c3c669d0a3c56e44c921bfeaf7fca9c7ee87e31e6f3a5ccec47303b231bbfd34ab5c6b9987403f898240af148caf9b77eb6f8df739344e538e680a17f3a69230"
        },
        {
            "65b41060556f1e9566e0b570a3dc293de5a84417af2f6496a8f6e8e42cbc23d0dd1aff22a8a57e5a1e9590190255d0da1305fad1825c14c92908e56b7a8902a0",
            "008dc0e03825c927c8a23a330c792b0d6a94e6ff4dd7ed5db497091199aed96ba4f361397c4768cf19989c4996df6b127d8b2be5bc9757830c7a2f36a191f5b4",
            "f49922bbd3ac3b2ce6cbf0ea8cb08509efe09a5e16e59f5c1ea02dc094d5ca575efb564a8492334a07ab8ed3b12d8ef38e8dfe6842561223f2cdc561dc6475ff"
        },
        {
            "5349fa1156207aac2d688bf3d28f0aba413d5794d85780a521ad390af048efc69805f02692e17a6ed7e7e78b2ff3adbd7fe7b72b70043af631e5dfee6f1134fe",
            "fad6ff8afe8393295eda4ee7ee905fb5fe935dcf3997f0cbcc915503b260d9768e62ce59488aabbb91cdf5fc49e075013fb7ed661a649c31dfd01a8eeba1b075",
            "4d7329894a231628e56cf40cc8c8010cde5f357856323d2065d127c03ea2a8901094f7e21600b5c0a45dbd2e39a4d5d6548f46721f27f40a139baed54e6de314"
        },
        {
            "2fda055d485b1104b1708f6bac78b36cd0fa3f61bf7d674d9f1689828095ff657593b1230f7023736a5c12d237b81c9527dd735bb1eaddbe8fa25fff9213b8e4",
            "47018689eefaa36160bd01bc2b4959802174d916f1928308789165fba848dccf171491a4fec59baef5afdcf898093c9311b126af9c9d7a909e64f39877b452ac",
            "2777ef3eae58b835b2393ca860c08a817176aad7f48251ae4790604312fbd241521e57cbf30c993455cab7752737c58036915d898d51d933894138b5da2511d3"
        },
        {
            "0ee3cf772e4249ed0237593fc512b347cc005f66e3f7acf98d9fc3c49fcbcef3d8c4fafd1391f3522b8b3202b2b42295bd4b11e3f7e38c69e3ecf12d53dd7874",
            "177fb5e23ec13c8c2be5343070021240f58710df11ffd21ab70b81c57718cc364c7d2d4848eb846276032a491b44c6e1474d1abe70b666eecbf3a602edde6f67",
            "4e6d0ced5d724a250cbc9b1cd08153ce581b1a2f07f2347a396fa9193ff929924109cf94c64c0c3f3acf93951e056b8f9d3abed229b86c13d9e3318dbfacf342"
        }
    };

    @Test
    public void testSeedEnglish()
    {
        for (int i = 0; i < MNEMONICS.length; i++)
        {
            String[] mnemonics = MNEMONICS[i].split(" ");
            for (int j = 0; j < PASSPHRASES.length; j++)
            {
                String exp = SEEDS_HEX[i][j];

                String res = Hex.encode(Bip39.seed(mnemonics, PASSPHRASES[j]));
                assertEquals(exp, res);
            }
        }
    }

    static String[] ENTROPY =
    {
        "00000000000000000000000000000000",
        "eab74859c61fb3ab3443b5fa2a9692c9caf51aab",
        "c99065de6c9614fba6e0632cc4a686b6422af0a5576d2d57",
        "da99f51f0fe553a9654fe2f26079d37d17e75d26966d0b11c1f66743",
        "94068fb7b052103a5c2aecf28b1d355cff62863867a026cc564cc28cf19202d4"
    };

    @Test
    public void testEntropyEnglish() throws Bip39.MnemonicLengthException, MnemonicWordException, MnemonicChecksumException
    {
        Bip39 bip39 = new Bip39();
        for (int i = 0; i < MNEMONICS.length; i++)
        {
            String[] mnemonics = MNEMONICS[i].split(" ");
            String exp = ENTROPY[i];
            String res = Hex.encode(bip39.entropy(mnemonics));
            assertEquals(exp, res, "i=" + i);
        }
    }

    @Test
    public void testMnemonicEnglish() throws Bip39.MnemonicLengthException, MnemonicWordException, MnemonicChecksumException
    {
        Bip39 bip39 = new Bip39();
        for (int i = 0; i < MNEMONICS.length; i++)
        {
            String exp = MNEMONICS[i];
            byte[] entropy = Hex.decode(ENTROPY[i]);
            String res = Strings.join(" ", bip39.mnemonic(entropy));
            assertEquals(exp, res, "i=" + i);
        }
    }

    @Test
    public void bip39TwelveWordsTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();

        String words = "absent essay fox snake vast pumpkin height crouch silent bulb excuse razor";

        bip39.check(words);
        byte[] seed = bip39.seed(words, "");

        assertEquals("727ecfcf0bce9d8ec0ef066f7aeb845c271bdd4ee06a37398cebd40dc810140bb620b6c10a8ad671afdceaf37aa55d92d6478f747e8b92430dd938ab5be961dd", Hex.encode(seed));
    }

    @Test
    public void bip39TwelveWordsInvalidTest()
    {
        Bip39 bip39 = new Bip39();

        try
        {
            String words = "absent absent absent absent absent absent absent absent absent absent absent absent";
            bip39.check(words);
            throw new AssertionError("an exception was expected but not thrown");
        }
        catch(Bip39.MnemonicException ex)
        {
            //do nothing, is expected
        }
    }

    @Test
    public void bip39TwelveWordsPassphraseTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "arch easily near social civil image seminar monkey engine party promote turtle";
        bip39.check(words);
        byte[] seed = bip39.seed(words, "anotherpass867");

        assertEquals("ca50764cda44a2cf52aef3c677bebf26011f9dc2b9fddfed2a8a5a9ecb8542956990a16e6873b7724044e83708d9d3a662b765e8800e6e79b289f51c2bcad756", Hex.encode(seed));
    }

    @Test
    public void bip39FifteenWordsTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "open grunt omit snap behave inch engine hamster hope increase exotic segment news choose roast";
        bip39.check(words);
        byte[] seed = bip39.seed(words, "");
        assertEquals("2174deae5fd315253dc065db7ef97f46957eb68a12505adccfb7f8aca5b63788c587e73430848f85417d9a7d95e6396d2eb3af73c9fb507ebcb9268a5ad47885", Hex.encode(seed));
    }

    @Test
    public void bip39EighteenWordsTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "mandate lend daring actual health dilemma throw muffin garden pony inherit volume slim visual police supreme bless crush";
        bip39.check(words);
        byte[] seed = bip39.seed(words, "");
        assertEquals("04bd65f582e288bbf595213048b06e1552017776d20ca290ac06d840e197bcaaccd4a85a45a41219be4183dd2e521e7a7a2d6aea3069f04e503ef6d9c8dfa651", Hex.encode(seed));
    }

    @Test
    public void bip39TwentyOneWordsTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "mirror milk file hope drill conduct empty mutual physical easily sell patient green final release excuse name asset update advance resource";
        bip39.check(words);
        byte[] seed = bip39.seed(words, "");
        assertEquals("f3a88a437153333f9759f323dfe7910e6a649c34da5800e6c978d77baad54b67b06eab17c0107243f3e8b395a2de98c910e9528127539efda2eea5ae50e94019", Hex.encode(seed));
    }

    @Test
    public void bip39TwentyFourWordsTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "earth easily dwarf dance forum muscle brick often huge base long steel silk frost quiz liquid echo adapt annual expand slim rookie venture oval";

        bip39.check(words);
        byte[] seed = bip39.seed(words, "");

        assertEquals("60f825219a1fcfa479de28435e9bf2aa5734e212982daee582ca0427ad6141c65be9863c3ce0f18e2b173083ea49dcf47d07148734a5f748ac60d470cee6a2bc", Hex.encode(seed));
    }

    @Test
    public void bip39TwentyFourWordsPassphraseTest() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "earth easily dwarf dance forum muscle brick often huge base long steel silk frost quiz liquid echo adapt annual expand slim rookie venture oval";

        bip39.check(words);
        byte[] seed = bip39.seed(words, "thispass");

        assertEquals("a652d123f421f56257391af26063e900619678b552dafd3850e699f6da0667269bbcaebb0509557481db29607caac0294b3cd337d740174cfa05f552fe9e0272", Hex.encode(seed));
    }

    @Test
    public void bip39TestVector1() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "letter advice cage absurd amount doctor acoustic avoid letter advice cage absurd amount doctor acoustic avoid letter always";

        bip39.check(words);
        byte[] seed = bip39.seed(words, "TREZOR");

        assertEquals("107d7c02a5aa6f38c58083ff74f04c607c2d2c0ecc55501dadd72d025b751bc27fe913ffb796f841c49b1d33b610cf0e91d3aa239027f5e99fe4ce9e5088cd65", Hex.encode(seed));
    }

    @Test
    public void bip39TestVector2() throws Bip39.MnemonicException
    {
        Bip39 bip39 = new Bip39();
        String words = "gravity machine north sort system female filter attitude volume fold club stay feature office ecology stable narrow fog";

        bip39.check(words);
        byte[] seed = bip39.seed(words, "TREZOR");

        assertEquals("628c3827a8823298ee685db84f55caa34b5cc195a778e52d45f59bcf75aba68e4d7590e101dc414bc1bbd5737666fbbef35d1f1903953b66624f910feef245ac", Hex.encode(seed));
    }

}
