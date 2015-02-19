package org.opendaylight.sfc.l2renderer.utils;

import java.util.Random;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;

import static com.fasterxml.uuid.EthernetAddress.constructMulticastAddress;
import static java.lang.Math.random;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createActionOutPort;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createActionPopVlan;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createActionPushVlan;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createActionSetDlDst;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createActionSetDlSrc;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createActionSetVlanId;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.addMatchVlan;


@RunWith(JUnitParamsRunner.class)
public class SfcOpenflowUtilsTest{

    private static Integer[] randNumArray= new Integer[6];
    private static Integer[] randVlanArray= new Integer[6];
    private static Long[] nextHopGroupIdArray = new Long[6];
    private static Random randomInt = new Random((int)random());
    static {
        for(int i = 0; i<randNumArray.length; i++){
            randNumArray[i] = (Integer)randomInt.nextInt((1500 - 0) + 1);
            randVlanArray[i] = (Integer)randomInt.nextInt((500 - 10) + 1) + 10;
            nextHopGroupIdArray[i] = Long.parseLong("" + (randomInt.nextInt((429496729 - 1000) + 1) + 1000));
        }
    }
    private static final Object[] createActionSetDlParams(){
        return $(
                $(constructMulticastAddress().toString(), randNumArray[0]),
                $(constructMulticastAddress().toString(), randNumArray[1]),
                $(constructMulticastAddress().toString(), randNumArray[2]),
                $(constructMulticastAddress().toString(), randNumArray[3]),
                $(constructMulticastAddress().toString(), randNumArray[4]),
                $(constructMulticastAddress().toString(), randNumArray[5])
        );
    }

    private static final Object[] createActionSetDlBadParams(){
        return $(
                $(constructMulticastAddress().toString().replace(":", ""), randNumArray[0]),
                $(constructMulticastAddress().toString().replace(":", "."), randNumArray[1]),
                $(constructMulticastAddress().toString().replace(":", RandomStringUtils.random(1, true, true)),
                        randNumArray[2]),
                $("                                                      ", randNumArray[3]),
                $(RandomStringUtils.random(17, true, true), randNumArray[4]),
                $(RandomStringUtils.random(randomInt.nextInt((150 - 1) + 1) + 1), randNumArray[5])
        );
    }

    private static final Object[] createActionOutPortParams(){
        return $(
        $(mock(Uri.class), randNumArray[0]),
        $(mock(Uri.class), randNumArray[1]),
        $(mock(Uri.class), randNumArray[2]),
        $(mock(Uri.class), randNumArray[3]),
        $(mock(Uri.class), randNumArray[4]),
        $(mock(Uri.class), randNumArray[5])
        );
    }
    private static final Object[] createActionVlanParams(){
        return $(
        $(randNumArray[0]),
        $(randNumArray[1]),
        $(randNumArray[2]),
        $(randNumArray[3]),
        $(randNumArray[4]),
        $(randNumArray[5])
        );
    }

    private static final Object[] createActionSetVlanIdParams() {
        return $(
                $(randVlanArray[0], randNumArray[0]),
                $(randVlanArray[1], randNumArray[1]),
                $(randVlanArray[2], randNumArray[2]),
                $(randVlanArray[3], randNumArray[3]),
                $(randVlanArray[4], randNumArray[4]),
                $(randVlanArray[5], randNumArray[5])
                );
    }

    private static final Object[] addMatchVlanParams(){
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
                "SetDlSrcActionCase [_setDlSrcAction=SetDlSrcAction [_address=MacAddress [_value=" + mac +
                        "], augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase",
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

/*    @Test
    @Parameters(method = "createSetDlActionBadParams")
    public void shouldThrowExceptionForCreateSetAction(String mac, int order) {
        //Test that badly formatted mac addresses cannot be used
        try {
            createSetDlSrcAction(mac, order);
        } catch (Exception e) {
            assertTrue("Exception is not instance of IllegalArgumentException", e instanceof  IllegalArgumentException);
            assertEquals("Error message does not match", "Supplied value \"" + mac + "\" " +
                    "does not match any of the permitted patterns [^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}$]", e.getMessage());
        }
    }*/

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
    public void testcreateActionOutPort(Uri uri, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createActionOutPort(uri.getValue(), order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "OutputActionCase [_outputAction=OutputAction [_outputNodeConnector=Mock for Uri, hashCode: "+ uri.hashCode()
                +", augmentation=[]], augmentation=[]]",
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
