/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow;

import java.util.Random;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;

import static com.fasterxml.uuid.EthernetAddress.constructMulticastAddress;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionOutPort;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionPopVlan;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionPushVlan;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionSetDlDst;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionSetDlSrc;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.createActionSetVlanId;
import static org.opendaylight.sfc.util.openflow.SfcOpenflowUtils.addMatchVlan;


@RunWith(JUnitParamsRunner.class)
public class SfcOpenflowUtilsTest{

    private static Integer[] randNumArray= new Integer[6];
    private static Integer[] randVlanArray= new Integer[6];
    private static Random randomInt = new Random();
    static {
        for(int i = 0; i<randNumArray.length; i++){
            randNumArray[i] = randomInt.nextInt((1500 - 0) + 1);
            randVlanArray[i] = randomInt.nextInt((500 - 10) + 1) + 10;
        }
    }
    @SuppressWarnings("unused")
    private static Object[] createActionSetDlParams(){
        return $(
                $(constructMulticastAddress().toString(), randNumArray[0]),
                $(constructMulticastAddress().toString(), randNumArray[1]),
                $(constructMulticastAddress().toString(), randNumArray[2]),
                $(constructMulticastAddress().toString(), randNumArray[3]),
                $(constructMulticastAddress().toString(), randNumArray[4]),
                $(constructMulticastAddress().toString(), randNumArray[5])
        );
    }

    @SuppressWarnings("unused")
    private static Object[] createActionSetDlBadParams(){
        return $(
                $(constructMulticastAddress().toString().replace(":", ""), randNumArray[0]),
                $(constructMulticastAddress().toString().replace(":", "."), randNumArray[1]),
                $(constructMulticastAddress().toString().replace(":", RandomStringUtils.random(1, true, true)),
                        randNumArray[2]),
                $("                                                      ", randNumArray[3]),
                $(RandomStringUtils.random(17, true, true), randNumArray[4]),
                $(cleanInvalidXmlChars(RandomStringUtils.random(randomInt.nextInt((150 - 1) + 1) + 1)), randNumArray[5])
        );
    }

    /**
     * Prevents using invalid xml chars as test input (they would break the surefire XML test result)
     * @param random the string to clean from invalid xml chars
     * @return the sanitized string
     */
    private static Object cleanInvalidXmlChars(String random) {
        String xml10pattern = "[^"
                + "\u0009\r\n"
                + "\u0020-\uD7FF"
                + "\uE000-\uFFFD"
                + "\ud800\udc00-\udbff\udfff"
                + "]";
        return random.replaceAll(xml10pattern, "");
    }

    @SuppressWarnings("unused")
    private static Object[] createActionOutPortParams(){
        return $(
        $(randNumArray[0].toString(), randNumArray[0]),
        $(randNumArray[1].toString(), randNumArray[1]),
        $(randNumArray[2].toString(), randNumArray[2]),
        $(randNumArray[3].toString(), randNumArray[3]),
        $(randNumArray[4].toString(), randNumArray[4]),
        $(randNumArray[5].toString(), randNumArray[5])
        );
    }
    @SuppressWarnings("unused")
    private static Object[] createActionVlanParams(){
        return $(
        $(randNumArray[0]),
        $(randNumArray[1]),
        $(randNumArray[2]),
        $(randNumArray[3]),
        $(randNumArray[4]),
        $(randNumArray[5])
        );
    }

    @SuppressWarnings("unused")
    private static Object[] createActionSetVlanIdParams() {
        return $(
                $(randVlanArray[0], randNumArray[0]),
                $(randVlanArray[1], randNumArray[1]),
                $(randVlanArray[2], randNumArray[2]),
                $(randVlanArray[3], randNumArray[3]),
                $(randVlanArray[4], randNumArray[4]),
                $(randVlanArray[5], randNumArray[5])
                );
    }

    @SuppressWarnings("unused")
    private static Object[] addMatchVlanParams(){
        return $(
        $(randVlanArray[0]),
        $(randVlanArray[1]),
        $(randVlanArray[2]),
        $(randVlanArray[3]),
        $(randVlanArray[4]),
        $(randVlanArray[5])
        );
    }

    @Test
    @Parameters(method = "createActionSetDlParams")
    public void testCreateActionSetDlSrc(String mac, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionSetDlSrc(mac, order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "SetFieldCase [_setField=SetField [_ethernetMatch=EthernetMatch [_ethernetSource=EthernetSource "
                + "[_address=MacAddress [_value=" + mac +
                        "], augmentation=[]], augmentation=[]], augmentation=[]], augmentation=[]]", testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong Order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionSetDlParams")
    public void testCreateActionSetDlDst(String mac, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionSetDlDst(mac, order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "SetFieldCase [_setField=SetField [_ethernetMatch=EthernetMatch [_ethernetDestination=EthernetDestination "
                + "[_address=MacAddress [_value=" + mac +
                        "], augmentation=[]], augmentation=[]], augmentation=[]], augmentation=[]]", testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionSetDlBadParams")
    public void shouldThrowExceptionForCreateSetAction(String mac, int order) {
        //Test that badly formatted mac addresses cannot be used
        try {
            createActionSetDlSrc(mac, order);
        } catch (Exception e) {
            assertTrue("Exception is not instance of IllegalArgumentException", e instanceof  IllegalArgumentException);
            assertEquals("Error message does not match", "Supplied value \"" + mac + "\" " +
                    "does not match required pattern \"^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}$\"", e.getMessage());
        }
    }

    @Test
    public void shouldThrowNPExceptionForCreateActionSet() {
        //Test that null cannot be used for mac address
        try {
            createActionSetDlSrc(null, randomInt.nextInt((1500 - 0) + 1) + 1);
        } catch (Exception e) {
            assertTrue("Exception is not instance of NullPointerException", e instanceof  NullPointerException);
            assertEquals("Error message does not match", "Supplied value may not be null", e.getMessage());
        }
    }

    @Test
    @Parameters(method = "createActionOutPortParams")
    public void testcreateActionOutPort(String uriStr, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionOutPort(uriStr, order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "OutputActionCase [_outputAction=OutputAction [_outputNodeConnector=Uri [_value="+ uriStr
                +"], augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionVlanParams")
    public void testCreateActionPushVlan(int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionPushVlan(order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "PushVlanActionCase [_pushVlanAction=PushVlanAction [_ethernetType=33024, augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionSetVlanIdParams")
    public void testCreateActionSetVlanId(int vlan, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionSetVlanId(vlan, order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "SetFieldCase [_setField=SetField [_vlanMatch=VlanMatch [_vlanId=VlanId [_vlanId=VlanId [_value=" + vlan
                + "], _vlanIdPresent=true, augmentation=[]], augmentation=[]], augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createActionVlanParams")
    public void testCreateActionPopVlan(int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionPopVlan(order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "PopVlanActionCase [_popVlanAction=PopVlanAction [augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "addMatchVlanParams")
    public void testAddMatchVlan(int vlan) {
        MatchBuilder match = new MatchBuilder();
        addMatchVlan(match, vlan);
        VlanMatch testAct = match.getVlanMatch();

        assertEquals("Wrong toString response",
                "VlanMatch [_vlanId=VlanId [_vlanId=VlanId [_value=" + vlan
                + "], _vlanIdPresent=true, augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch",
                testAct.getImplementedInterface().getName());
    }

}
