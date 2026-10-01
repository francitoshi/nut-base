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

import io.nut.base.encoding.Base58;
import io.nut.base.encoding.Hex;
import java.security.InvalidKeyException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class BipsTest
{
    static final String WORDS = "skin sketch rice sudden polar twin guard demand perfect chaos enlist kick";

    static final String[][] DATA =
    {
        {
            "Legacy", "m/44h/0h/0h/0",
            "xprvA2Ad9RzSEng3MtS6NRmnRwn5spY1AFQ9T1E4AEjE4e3chAJ1Uhk9V9jYegmyGVNCsijK6jwgj1T3KLPfEwZDHqa7khrXqfynPrddAysn1X7",
            "xpub6F9yYwXL5AELaNWZUTJno5ipRrNVZi7zpE9exd8qcyabZxdA2F4Q2x42VwbcjirsPX1dhEkAScEpegWje9syxCj6bTwzXYyYzX6NroDgK52",
            "1842DZNznxbaNK4pSPQ6bcwLbyS3SaTfoi", "1AGksJ2uKYxXZFv75qwuVsByFDV9d1s3zL", "19yjxkbXCoCsjf254Vo35eLLCK4nNLg7RZ"
        },
        {
            "Legacy", "m/44h/0h/0h/1",
            "xprvA2Ad9RzSEng3PkQuNv5cHsBbZ6qscoPMLBZGwyUMwH9THKpfbwBzfhYz9SLTd3v8jjkG7C2MNMMdW3WeabhPgVyKr3YfRgPkcWNfEJPSFHZ",
            "xpub6F9yYwXL5AELcEVNUwccf18L78gN2G7ChQUskMsyVcgSA89p9UWFDVsTzjaGMJptaUz7XxXr16sNKUckDTN9DxXCwoY4FfnDHGjQbXmXRrX"
        },
        {
            "Legacy", "m/44h/0h/1h/2",
            "xprv9zgGjbq2xA7GDhq8khFYSvDVvuxy91LkmxTsLHiMHDE5kETMxyXKXn9e3BqmAufYy7Rjtwsv3TFu5yH6poDQFJeLwNznHe7iqztDv5TUWVA",
            "xpub6Dfd97MvnXfZSBubrinYp4AEUwoTYU4c9BPU8g7xqYm4d2nWWWqa5aU7tTbZnx8Gccf9L4YtTvNqrjYBXFGukoACLq4EiHjBSJHRYuqW6yN"
        },
        {
            "NestedSegwit", "m/49h/0h/0h/0",
            "yprvAKom7JiBvkW4EtjeCrCJYDRfegHzpCDR9BPAR1wbc8zbTo7CSZvsoetavjnn64R5AvPUKV3csp2NmjQjXjiNBKV4LKuU1EMQHRvbteLwyb7",
            "ypub6Yo7WpF5m84MTNp7JsjJuMNQCi8VDewGWQJmDQMDAUXaLbSLz7F8MTD4n3Y5Sri6ZuNmWbqWZQqsqBScpf1tJE1YDHzm6jHvPvAkxWPoFq8"
        },
        {
            "NativeSegwit", "m/84'/0'/0'/0",
            "zprvAfYdVszgvfc8uS14achYe9mjB1QLqs4Dr8j5HHZ8NAhoXYgg1jbeWuzQzUA7djxUr4SYMC1FSNYByPcxvU273Drxwd2oNPdF2NXMmdMyrij",
            "zpub6tXyuPXam3AS7v5XgeEZ1HiTj3EqFKn5DMeg5fxjvWEnQM1pZGuu4iJtqikLQHNx9GFYhF22DhvkpYs2R4EVCXF7AMF16CM7JoRuS3NMggv"
        },
        {
            "Taproot", "m/86'/0'/0'",
            null,
            "xpub6C7BKDRBmBfjmmcJWQdqcLoiewHjV9DDxERPX6oWVo9GE5MUToySGkcxVQabW6pEzKXG81xMqXqpfvrg3dGdkA2wmsP9RgjRYAt5PDtEV9H"
        },
        {
            "Taproot", "m/86'/0'/1'",
            null,
            "xpub6C7BKDRBmBfjqNGH4eeropx6s9PEJrhsUdgZwGHh2CXbNa8bVruWa1iteGD2LnqWhg8XMG2wo138YtESeh6py2P8QSNNQ9zkR1jQcJfobyp"
        }
    };
    
    @Test
    public void bip39TestVector3() throws Bip39.MnemonicException, InvalidKeyException, Base58.FormatException
    {
        Bip39 bip39 = new Bip39();

        bip39.check(WORDS);
        byte[] seed = bip39.seed(WORDS, "");

        for(String[] items : DATA)
        {
            String type = items[0];
            String path = items[1];
            String pathXprv = items[2];
            String pathXpub = items[3];

            ExtKey.ScriptType scriptType = ExtKey.ScriptType.valueOf(type);
            Bip32 bip32 = Bip32.build(seed, scriptType, ExtKey.PolicyType.SingleSig, ExtKey.Network.MainNet, true);

            int[] childNumbers = Bip32.parsePath(path);

            if(pathXprv!=null) 
            {
                assertEquals(pathXprv, bip32.xprv(childNumbers).toString(), path+" xprv");
                assertEquals(pathXprv, bip32.xprv(path).toString(), path+" xprv");
            }
            assertEquals(pathXpub, bip32.xpub(childNumbers).toString(), path+" xpub");
            assertEquals(pathXpub, bip32.xpub(path).toString(), path+" xpub");

            for(int i=0;i<items.length-4;i++)
            {
                byte[] addr = bip32.addr(childNumbers, i);
                String a = Base58.encode(addr);
                System.out.println(a);
            }
        }
    }
    
    
    @Test
    public void bip86Test() throws Bip39.MnemonicException, InvalidKeyException, Base58.FormatException
    {
        //https://en.bitcoin.it/wiki/BIP_0086
        Bip39 bip39 = new Bip39();

        String MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        byte[] seed = bip39.seed(bip39.check(MNEMONIC), "");
        Bip32 bip32 = Bip32.build(seed, ExtKey.ScriptType.Taproot, ExtKey.PolicyType.SingleSig, ExtKey.Network.MainNet, true);

        String rootprv = "xprv9s21ZrQH143K3GJpoapnV8SFfukcVBSfeCficPSGfubmSFDxo1kuHnLisriDvSnRRuL2Qrg5ggqHKNVpxR86QEC8w35uxmGoggxtQTPvfUu";
        String rootpub = "xpub661MyMwAqRbcFkPHucMnrGNzDwb6teAX1RbKQmqtEF8kK3Z7LZ59qafCjB9eCRLiTVG3uxBxgKvRgbubRhqSKXnGGb1aoaqLrpMBDrVxga8";

        assertEquals(rootprv, bip32.xprv().toString(),"ROOTPRV");
        assertEquals(rootpub, bip32.xpub().toString(),"ROOTPUB");

        {
            String path = "m/86'/0'/0'";
            String xprv = "xprv9xgqHN7yz9MwCkxsBPN5qetuNdQSUttZNKw1dcYTV4mkaAFiBVGQziHs3NRSWMkCzvgjEe3n9xV8oYywvM8at9yRqyaZVz6TYYhX98VjsUk";
            String xpub = "xpub6BgBgsespWvERF3LHQu6CnqdvfEvtMcQjYrcRzx53QJjSxarj2afYWcLteoGVky7D3UKDP9QyrLprQ3VCECoY49yfdDEHGCtMMj92pReUsQ";
            
            assertEquals(xprv, bip32.xprv(path).toString(),"xprv "+path);
            assertEquals(xpub, bip32.xpub(path).toString(),"xpub "+path);
        }
        {
            String path = "m/86'/0'/0'/0/0";
            String xprv = "xprvA449goEeU9okwCzzZaxiy475EQGQzBkc65su82nXEvcwzfSskb2hAt2WymrjyRL6kpbVTGL3cKtp9herYXSjjQ1j4stsXXiRF7kXkCacK3T";
            String xpub = "xpub6H3W6JmYJXN49h5TfcVjLC3onS6uPeUTTJoVvRC8oG9vsTn2J8LwigLzq5tHbrwAzH9DGo6ThGUdWsqce8dGfwHVBxSbixjDADGGdzF7t2B";

            assertEquals(xprv, bip32.xprv(path).toString(),"xprv "+path);
            assertEquals(xpub, bip32.xpub(path).toString(),"xpub "+path);

//            String internal_key = "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
//            String output_key   = "a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
//            String scriptPubKey = "5120a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
            String address      = "bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr";

            System.out.println(Hex.encode(bip32.addr(path)));
            System.out.println(Base58.encode(bip32.addr(path)));
//            assertEquals(address, bip32.addr(path),"xpub "+path);

        }
        {
        // Account 0, second receiving address = m/86'/0'/0'/0/1
        String xprv         = "xprvA449goEeU9okyiF1LmKiDaTgeXvmh87DVyRd35VPbsSop8n8uALpbtrUhUXByPFKK7C2yuqrB1FrhiDkEMC4RGmA5KTwsE1aB5jRu9zHsuQ";
        String xpub         = "xpub6H3W6JmYJXN4CCKUSnriaiQRCZmG6aq4sCMDqTu1ACyngw7HShf59hAxYjXgKDuuHThVEUzdHrc3aXCr9kfvQvZPit5dnD3K9xVRBzjK3rX";
//        internal_key = "83dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145";
//        output_key   = "a82f29944d65b86ae6b5e5cc75e294ead6c59391a1edc5e016e3498c67fc7bbb";
//        scriptPubKey = "5120a82f29944d65b86ae6b5e5cc75e294ead6c59391a1edc5e016e3498c67fc7bbb";
//        address      = "bc1p4qhjn9zdvkux4e44uhx8tc55attvtyu358kutcqkudyccelu0was9fqzwh";
        }
        {
        // Account 0, first change address = m/86'/0'/0'/1/0
        String xprv         = "xprvA3Ln3Gt3aphvUgzgEDT8vE2cYqb4PjFfpmbiFKphxLg1FjXQpkAk5M1ZKDY15bmCAHA35jTiawbFuwGtbDZogKF1WfjwxML4gK7WfYW5JRP";
        String xpub         = "xpub6GL8SnQwRCGDhB59LEz9HMyM6sRYoByXBzXK3iEKWgCz8XrZNHUzd9L3AUBELW5NzA7dEFvMas1F84TuPH3xqdUA5tumaGWFgihJzWytXe3";
//        internal_key = "399f1b2f4393f29a18c937859c5dd8a77350103157eb880f02e8c08214277cef";
//        output_key   = "882d74e5d0572d5a816cef0041a96b6c1de832f6f9676d9605c44d5e9a97d3dc";
//        scriptPubKey = "5120882d74e5d0572d5a816cef0041a96b6c1de832f6f9676d9605c44d5e9a97d3dc";
//        address      = "bc1p3qkhfews2uk44qtvauqyr2ttdsw7svhkl9nkm9s9c3x4ax5h60wqwruhk7";
        }
        
        
        System.out.println(bip32.xprv());
//
//        int[] childNumbers = Bip32.parsePath(path);
//            ExtKey xprv = bip32.xprv(childNumbers);
//            ExtKey xpub = bip32.xpub(childNumbers);
//
//            if(pathXprv!=null) 
//            {
//                assertEquals(pathXprv, xprv.toString(),path+" xprv");
//            }
//            assertEquals(pathXpub, xpub.toString(),path+" xpub");
//
//            for(int i=0;i<items.length-4;i++)
//            {
//                byte[] addr = bip32.addr(childNumbers, i);
//                String a = Base58.encode(addr);
//                System.out.println(a);
//            }
//        }

        
        
    }
}
