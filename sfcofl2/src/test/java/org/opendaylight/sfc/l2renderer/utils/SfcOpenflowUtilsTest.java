package org.opendaylight.sfc.l2renderer.utils;

import java.util.Random;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;

import static com.fasterxml.uuid.EthernetAddress.constructMulticastAddress;
import static java.lang.Math.random;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createOutputAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createPopVlanAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createPushVlanAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createSetDlDstAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createSetDlSrcAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createSetDstVlanAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createSetGroupAction;
import static org.opendaylight.sfc.l2renderer.utils.SfcOpenflowUtils.createVlanMatch;


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
    private static final Object[] createSetDlActionParams(){
        return $(
                $(constructMulticastAddress().toString(), randNumArray[0]),
                $(constructMulticastAddress().toString(), randNumArray[1]),
                $(constructMulticastAddress().toString(), randNumArray[2]),
                $(constructMulticastAddress().toString(), randNumArray[3]),
                $(constructMulticastAddress().toString(), randNumArray[4]),
                $(constructMulticastAddress().toString(), randNumArray[5])
        );
    }

    private static final Object[] createSetDlActionBadParams(){
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

    private static final Object[] createOutputActionParams(){
        return $(
        $(mock(Uri.class), randNumArray[0]),
        $(mock(Uri.class), randNumArray[1]),
        $(mock(Uri.class), randNumArray[2]),
        $(mock(Uri.class), randNumArray[3]),
        $(mock(Uri.class), randNumArray[4]),
        $(mock(Uri.class), randNumArray[5])
        );
    }
    private static final Object[] createVlanActionParams(){
        return $(
        $(randNumArray[0]),
        $(randNumArray[1]),
        $(randNumArray[2]),
        $(randNumArray[3]),
        $(randNumArray[4]),
        $(randNumArray[5])
        );
    }

    private static final Object[] createSetDstVlanActionParams() {
        return $(
                $(randVlanArray[0], randNumArray[0]),
                $(randVlanArray[1], randNumArray[1]),
                $(randVlanArray[2], randNumArray[2]),
                $(randVlanArray[3], randNumArray[3]),
                $(randVlanArray[4], randNumArray[4]),
                $(randVlanArray[5], randNumArray[5])
                );
    }

    private static final Object[] createSetGroupActionParams() {
        return $(
                $(nextHopGroupIdArray[0], randNumArray[0]),
                $(nextHopGroupIdArray[1], randNumArray[1]),
                $(nextHopGroupIdArray[2], randNumArray[2]),
                $(nextHopGroupIdArray[3], randNumArray[3]),
                $(nextHopGroupIdArray[4], randNumArray[4]),
                $(nextHopGroupIdArray[5], randNumArray[5])
                );
    }

    private static final Object[] createVlanMatchActionParams(){
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
    @Parameters(method = "createSetDlActionParams")
    public void testCreateSetDlSrcAction(String mac, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createSetDlSrcAction(mac, order);
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
    @Parameters(method = "createSetDlActionParams")
    public void testCreateSetDlDstAction(String mac, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createSetDlDstAction(mac, order);
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
    public void shouldThrowNPExceptionForCreateSetAction() {
        //Test that null cannot be used for mac address
        try {
            createSetDlSrcAction(null, randomInt.nextInt((1500 - 0) + 1) + 1);
        } catch (Exception e) {
            assertTrue("Exception is not instance of NullPointerException", e instanceof  NullPointerException);
            assertEquals("Error message does not match", "Supplied value may not be null", e.getMessage());
        }
    }

    @Test
    @Parameters(method = "createOutputActionParams")
    public void testCreateOutputAction(Uri uri, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createOutputAction(uri, order);
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
    @Parameters(method = "createVlanActionParams")
    public void testCreatePushVlanAction(int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createPushVlanAction(order);
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
    @Parameters(method = "createSetDstVlanActionParams")
    public void testCreateSetDstVlanAction(int vlan, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createSetDstVlanAction(vlan, order);
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
    @Parameters(method = "createVlanActionParams")
    public void testCreatePopVlanAction(int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createPopVlanAction(order);
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
    @Parameters(method = "createSetGroupActionParams")
    public void testCreateSetGroupAction(Long nextHopGroupId, int order) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action testActList =
                createSetGroupAction(nextHopGroupId, order);
        Action testAct = testActList.getAction();

        assertEquals("Wrong toString response",
                "GroupActionCase [_groupAction=GroupAction [_groupId=" + nextHopGroupId
                + ", augmentation=[]], augmentation=[]]", testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase",
                testAct.getImplementedInterface().getName());
        assertEquals("Wrong order", new Integer(order), testActList.getOrder());
    }

    @Test
    @Parameters(method = "createVlanMatchActionParams")
    public void testCreateVlanMatch(int vlan) {
        VlanMatch testAct = createVlanMatch(vlan);

        assertEquals("Wrong toString response",
                "VlanMatch [_vlanId=VlanId [_vlanId=VlanId [_value=" + vlan
                + "], _vlanIdPresent=true, augmentation=[]], augmentation=[]]",
                testAct.toString());
        assertEquals("Wrong action type",
                "org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch",
                testAct.getImplementedInterface().getName());
    }

}
