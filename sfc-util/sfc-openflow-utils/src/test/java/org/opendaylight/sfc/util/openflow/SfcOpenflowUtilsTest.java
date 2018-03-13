/*
 * Copyright (c) 2015, 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow;

import static com.fasterxml.uuid.EthernetAddress.constructMulticastAddress;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.addMatchVlan;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionOutPort;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionPopVlan;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionPushVlan;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionSetDlDst;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionSetDlSrc;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionSetVlanId;

import java.math.BigInteger;
import java.util.Random;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnitParamsRunner.class)
public class SfcOpenflowUtilsTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOpenflowUtilsTest.class);

    private static final int ETHERTYPE_VLAN = 33024;
    private static final int RANDOM_PATTERNS_GENERATED_FOR_TEST_CREATE_ACTIONS = 100;
    private static final int TOTAL_PATTERNS_GENERATED_FOR_TEST_CREATE_ACTIONS =
            5 + RANDOM_PATTERNS_GENERATED_FOR_TEST_CREATE_ACTIONS;

    private static Integer[] randNumArray = new Integer[TOTAL_PATTERNS_GENERATED_FOR_TEST_CREATE_ACTIONS];
    private static Integer[] randVlanArray = new Integer[6];
    private static Random randomInt = new Random();

    static {
        for (int i = 0; i < randVlanArray.length; i++) {
            randVlanArray[i] = randomInt.nextInt(500 - 10 + 1) + 10;
        }
        for (int i = 0; i < randNumArray.length; i++) {
            randNumArray[i] = randomInt.nextInt(1500 - 0 + 1);
        }
    }

    @SuppressWarnings("unused")
    private static Object[] createActionSetDlParams() {
        return $($(constructMulticastAddress().toString(), randNumArray[0]),
                $(constructMulticastAddress().toString(), randNumArray[1]),
                $(constructMulticastAddress().toString(), randNumArray[2]),
                $(constructMulticastAddress().toString(), randNumArray[3]),
                $(constructMulticastAddress().toString(), randNumArray[4]),
                $(constructMulticastAddress().toString(), randNumArray[5]));
    }

    @SuppressWarnings("unused")
    private static Object[] createActionSetDlBadParams() {
        Object [] generatedInput = new Object[TOTAL_PATTERNS_GENERATED_FOR_TEST_CREATE_ACTIONS];

        generatedInput[0] = new Object[] {constructMulticastAddress().toString().replace(":", ""), randNumArray[0]};
        generatedInput[1] = new Object[] {constructMulticastAddress().toString().replace(":", "."), randNumArray[1]};
        generatedInput[2] = new Object[] {
                constructMulticastAddress().toString().replace(":", RandomStringUtils.random(1, true, true)),
                randNumArray[2]
        };
        generatedInput[3] = new Object[] {"                                                      ", randNumArray[3]};

        int retries = 0;
        for (int i = 4; i < generatedInput.length; i++) {
            String input = RandomStringUtils.random(randomInt.nextInt(150 - 1 + 1) + 1, true, true);
            String cleaned = cleanInvalidXmlChars(input);
            generatedInput[i] = new Object[] {cleaned, randNumArray[i]};
        }
        return generatedInput;
    }

    /**
     * Prevents using invalid xml chars as test input (they would break the
     * surefire XML test result).
     *
     * @param random
     *            the string to clean from invalid xml chars
     * @return the sanitized string
     */
    @SuppressWarnings({"checkstyle:IllegalTokenText", "checkstyle:AvoidEscapedUnicodeCharacters"})
    private static String cleanInvalidXmlChars(String random) {
        String xml10pattern = "[^" + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff"
                + "]";
        return random.replaceAll(xml10pattern, "");
    }

    @SuppressWarnings("unused")
    private static Object[] createActionOutPortParams() {
        return $($(randNumArray[0].toString(), randNumArray[0]), $(randNumArray[1].toString(), randNumArray[1]),
                $(randNumArray[2].toString(), randNumArray[2]), $(randNumArray[3].toString(), randNumArray[3]),
                $(randNumArray[4].toString(), randNumArray[4]), $(randNumArray[5].toString(), randNumArray[5]));
    }

    @SuppressWarnings("unused")
    private static Object[] createActionVlanParams() {
        return $($(randNumArray[0]), $(randNumArray[1]), $(randNumArray[2]), $(randNumArray[3]), $(randNumArray[4]),
                $(randNumArray[5]));
    }

    @SuppressWarnings("unused")
    private static Object[] createActionSetVlanIdParams() {
        return $($(randVlanArray[0], randNumArray[0]), $(randVlanArray[1], randNumArray[1]),
                $(randVlanArray[2], randNumArray[2]), $(randVlanArray[3], randNumArray[3]),
                $(randVlanArray[4], randNumArray[4]), $(randVlanArray[5], randNumArray[5]));
    }

    @SuppressWarnings("unused")
    private static Object[] addMatchVlanParams() {
        return $($(randVlanArray[0]), $(randVlanArray[1]), $(randVlanArray[2]), $(randVlanArray[3]),
                $(randVlanArray[4]), $(randVlanArray[5]));
    }

    @Test
    @Parameters(method = "createActionSetDlParams")
    public void testCreateActionSetDlSrc(String mac, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action
            .list.Action testActList = createActionSetDlSrc(
                mac, order);
        Action testAct = testActList.getAction();

        // org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase
        assertTrue(testAct instanceof SetFieldCase);
        assertEquals("Wrong Src Mac",
                ((SetFieldCase)testAct).getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue(),
                mac);
        assertEquals("Wrong Order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionSetDlParams")
    public void testCreateActionSetDlDst(String mac, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list
            .Action testActList = createActionSetDlDst(mac, order);
        Action testAct = testActList.getAction();

        assertTrue(testAct instanceof SetFieldCase);
        assertEquals("Wrong Dst Mac",
            ((SetFieldCase)testAct).getSetField().getEthernetMatch().getEthernetDestination().getAddress().getValue(),
            mac);
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionSetDlBadParams")
    public void shouldThrowExceptionForCreateSetAction(String mac, int order) {
        // Test that badly formatted MAC addresses cannot be used
        try {
            createActionSetDlSrc(mac, order);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message does not match",
                    e.getMessage().contains("Supplied value \"" + mac + "\" does not match required pattern")
                            && e.getMessage().contains("[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}"));
        }
    }

    @Test
    public void shouldThrowNPExceptionForCreateActionSet() {
        // Test that null cannot be used for MAC address
        try {
            createActionSetDlSrc(null, randomInt.nextInt(1500 - 0 + 1) + 1);
        } catch (NullPointerException e) {
            assertEquals("Error message does not match", "Supplied value may not be null", e.getMessage());
        }
    }

    @Test
    @Parameters(method = "createActionOutPortParams")
    public void testcreateActionOutPort(String uriStr, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action
            .list.Action testActList = createActionOutPort(uriStr, order);
        Action testAct = testActList.getAction();

        assertTrue(testAct instanceof OutputActionCase);
        assertEquals("Wrong uriStr",
                ((OutputActionCase) testAct).getOutputAction().getOutputNodeConnector().getValue(), uriStr);
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionVlanParams")
    public void testCreateActionPushVlan(int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list
            .Action testActList = createActionPushVlan(order);
        Action testAct = testActList.getAction();

        assertTrue(testAct instanceof PushVlanActionCase);
        assertEquals(((PushVlanActionCase) testAct).getPushVlanAction().getEthernetType().intValue(), ETHERTYPE_VLAN);
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionSetVlanIdParams")
    public void testCreateActionSetVlanId(int vlan, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action
            .list.Action testActList = createActionSetVlanId(vlan, order);
        Action testAct = testActList.getAction();

        assertTrue(testAct instanceof SetFieldCase);
        assertEquals("Wrong VlanId",
                ((SetFieldCase) testAct).getSetField().getVlanMatch().getVlanId().getVlanId().getValue().intValue(),
                vlan);
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionVlanParams")
    public void testCreateActionPopVlan(int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112
            .action.list.Action testActList = createActionPopVlan(order);
        Action testAct = testActList.getAction();

        assertTrue(testAct instanceof PopVlanActionCase);
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "addMatchVlanParams")
    public void testAddMatchVlan(int vlan) {
        MatchBuilder match = new MatchBuilder();
        addMatchVlan(match, vlan);
        VlanMatch testAct = match.getVlanMatch();

        assertEquals("Wrong VlanId", vlan, testAct.getVlanId().getVlanId().getValue().intValue());
        assertTrue(testAct instanceof VlanMatch);
    }

    @Test
    @Parameters(method = "bigIntegerToMacConversionsParams")
    public void testBigIntegerToMacStringConversions(BigInteger bi, String expectedValue) {
        Assert.assertEquals("bad bigint to mac format conversion!", SfcOpenflowUtils.macStringFromBigInteger(bi),
                expectedValue);
    }

    public Object[][] bigIntegerToMacConversionsParams() {
        final BigInteger number256 = new BigInteger("256");
        final BigInteger MAX_MAC = number256.multiply(number256).multiply(number256).multiply(number256)
                .multiply(number256).multiply(number256).subtract(new BigInteger("1"));

        return new Object[][] { { new BigInteger("0"), "00:00:00:00:00:00" },
                                { new BigInteger("1"), "00:00:00:00:00:01" },
                                { new BigInteger("15"), "00:00:00:00:00:0f" },
                                { new BigInteger("16"), "00:00:00:00:00:10" },
                                { new BigInteger("17"), "00:00:00:00:00:11" },
                                { new BigInteger("255"), "00:00:00:00:00:ff" },
                                { new BigInteger("256"), "00:00:00:00:01:00" },
                                { new BigInteger("257"), "00:00:00:00:01:01" },
                                { new BigInteger(new Integer(256 * 256).toString()), "00:00:00:01:00:00" },
                                { MAX_MAC, "ff:ff:ff:ff:ff:ff" } };
    }
}
