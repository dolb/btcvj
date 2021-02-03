/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.Networks;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.script.Script.ScriptType;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class LegacyAddressTest {
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final NetworkParameters MAINNET = MainNetParams.get();

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(LegacyAddress.class)
                .withPrefabValues(NetworkParameters.class, MAINNET, TESTNET)
                .suppress(Warning.NULL_FIELDS)
                .suppress(Warning.TRANSIENT_FIELDS)
                .usingGetClass()
                .verify();
    }

    @Test
    public void testJavaSerialization() throws Exception {
        LegacyAddress testAddress = LegacyAddress.fromBase58(TESTNET, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testAddress);
        LegacyAddress testAddressCopy = (LegacyAddress) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()))
                .readObject();
        assertEquals(testAddress, testAddressCopy);

        LegacyAddress mainAddress = LegacyAddress.fromBase58(MAINNET, "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainAddress);
        LegacyAddress mainAddressCopy = (LegacyAddress) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()))
                .readObject();
        assertEquals(mainAddress, mainAddressCopy);
    }

    @Test
    public void stringification() {
        // Test a testnet address.
        LegacyAddress a = LegacyAddress.fromPubKeyHash(TESTNET, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toString());
        assertEquals(ScriptType.P2PKH, a.getOutputScriptType());

        LegacyAddress b = LegacyAddress.fromPubKeyHash(MAINNET, HEX.decode("03b7396b1f0f08c4813d292ab3235dce12cc5c55"));
        assertEquals("YPhhKh9d32yY89effx7mcHdquqyq4G1AL5", b.toString());
        assertEquals(ScriptType.P2PKH, b.getOutputScriptType());
    }

    @Test
    public void decoding() {
        LegacyAddress a = LegacyAddress.fromBase58(TESTNET, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.HEX.encode(a.getHash()));

        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
        assertEquals("03b7396b1f0f08c4813d292ab3235dce12cc5c55", Utils.HEX.encode(b.getHash()));
    }

    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            LegacyAddress.fromBase58(TESTNET, "this is not a valid address!");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            LegacyAddress.fromBase58(TESTNET, "");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            LegacyAddress.fromBase58(TESTNET, "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            // Success.
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() {
        NetworkParameters params = LegacyAddress.getParametersFromAddress("YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
        assertEquals(MAINNET.getId(), params.getId());
        params = LegacyAddress.getParametersFromAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals(TESTNET.getId(), params.getId());
    }

    @Test
    public void getAltNetwork() {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        NetworkParameters params = LegacyAddress.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = LegacyAddress.getParametersFromAddress("YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
        assertEquals(MAINNET.getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            LegacyAddress.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) { }
    }

    @Test
    public void p2shAddress() {
        // Test that we can construct P2SH addresses
        LegacyAddress mainNetP2SHAddress = LegacyAddress.fromBase58(MainNetParams.get(), "RHhWYBs5RGDHmMbRkx4wXZd9azciWXLz4K");
        assertEquals(mainNetP2SHAddress.getVersion(), MAINNET.p2shHeader);
        assertEquals(ScriptType.P2SH, mainNetP2SHAddress.getOutputScriptType());
        LegacyAddress testNetP2SHAddress = LegacyAddress.fromBase58(TestNet3Params.get(), "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(testNetP2SHAddress.getVersion(), TESTNET.p2shHeader);
        assertEquals(ScriptType.P2SH, testNetP2SHAddress.getOutputScriptType());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = LegacyAddress.getParametersFromAddress("RHhWYBs5RGDHmMbRkx4wXZd9azciWXLz4K");
        assertEquals(MAINNET.getId(), mainNetParams.getId());
        NetworkParameters testNetParams = LegacyAddress.getParametersFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(TESTNET.getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("5c5a661c6538497a4d91f4c8955d8a44e3cc25bd");
        LegacyAddress a = LegacyAddress.fromScriptHash(MAINNET, hex);
        assertEquals("RHhWYBs5RGDHmMbRkx4wXZd9azciWXLz4K", a.toString());
        LegacyAddress b = LegacyAddress.fromScriptHash(TESTNET, HEX.decode("18a0e827269b5211eb51a4af1b2fa69333efa722"));
        assertEquals("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe", b.toString());
        LegacyAddress c = LegacyAddress.fromScriptHash(MAINNET,
                ScriptPattern.extractHashFromP2SH(ScriptBuilder.createP2SHOutputScript(hex)));
        assertEquals("RHhWYBs5RGDHmMbRkx4wXZd9azciWXLz4K", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKey key1 = DumpedPrivateKey.fromBase58(MAINNET, "5JaTXbAUmfPYZFRwrYaALK48fN6sFJp4rHqq2QSXs8ucfpE4yQU").getKey();
        key1 = ECKey.fromPrivate(key1.getPrivKeyBytes());
        ECKey key2 = DumpedPrivateKey.fromBase58(MAINNET, "5Jb7fCeh1Wtm4yBBg3q3XbT6B525i17kVhy3vMC9AqfR6FH2qGk").getKey();
        key2 = ECKey.fromPrivate(key2.getPrivKeyBytes());
        ECKey key3 = DumpedPrivateKey.fromBase58(MAINNET, "5JFjmGo5Fww9p8gvx48qBYDJNAzR9pmH5S389axMtDyPT8ddqmw").getKey();
        key3 = ECKey.fromPrivate(key3.getPrivKeyBytes());

        List<ECKey> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys);
        LegacyAddress address = LegacyAddress.fromScriptHash(MAINNET,
                ScriptPattern.extractHashFromP2SH(p2shScript));
        assertEquals("RVcG2YaugNWFQfMyX1fQt2JjyoPuoA3wZr", address.toString());
    }

    @Test
    public void cloning() throws Exception {
        LegacyAddress a = LegacyAddress.fromPubKeyHash(TESTNET, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        LegacyAddress b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() {
        String base58 = "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5";
        assertEquals(base58, LegacyAddress.fromBase58(null, base58).toBase58());
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "YSou9gHDeFPTzAqkvB1gLLNZybo5uypf92");
        LegacyAddress b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonLessThan() {
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "YSou9gHDeFPTzAqkvB1gLLNZybo5uypf92");

        int result = a.compareTo(b);
        assertTrue(result < 0);
    }

    @Test
    public void comparisonGreaterThan() {
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "YSou9gHDeFPTzAqkvB1gLLNZybo5uypf92");
        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");

        int result = a.compareTo(b);
        assertTrue(result > 0);
    }

    @Test
    public void comparisonNotEquals() {
        // These addresses only differ by version byte
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "YPhhKh9d32yY89effx7mcHdquqyq4G1AL5");
        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "RD38s6awnRnnbsHGsWK4Gz2ViG9sgE9ZZK");

        int result = a.compareTo(b);
        assertTrue(result != 0);
    }
}
