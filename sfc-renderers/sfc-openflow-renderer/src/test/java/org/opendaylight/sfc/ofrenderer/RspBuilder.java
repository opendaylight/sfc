/*
 * Copyright (c) 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtilsTestMock;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Nsh;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SfcEncapsulationIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MplsBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPortBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;


public class RspBuilder {

    private static String SF_NAME_PREFIX = "SF_";
    private static String SFF_NAME_PREFIX = "SFF_";
    private static String SFF_DPL_NAME_PREFIX = "SFF_DPL_";
    private static String SFC_NAME_PREFIX = "SFC_";
    private static String SFP_NAME_PREFIX = "SFP_";
    private static String RSP_NAME_PREFIX = "RSP_";
    private static String MAC_ADDRESS_PREFIX = "00:00:00:00:00:";
    private static String IP_ADDRESS_PREFIX = "192.168.0.";
    private static int VLAN_BASE = 1;
    private static int MPLS_BASE = 1;
    private static int VXLAN_UDP_PORT = 6633;
    private static String SWITCH_PORT_STR = "1";

    private long RSP_PATHID_INDEX = 0;
    private int SF_NAME_INDEX = 0;
    private int SFF_NAME_INDEX = 0;
    private int SFF_DPL_NAME_INDEX = 0;
    private int SFC_NAME_INDEX = 0;
    private int SFP_NAME_INDEX = 0;
    private int RSP_NAME_INDEX = 0;
    private int MAC_ADDR_INDEX = 0;
    private int IP_ADDR_INDEX = 1;
    private int VLAN_SFF_INDEX = 0;
    private int VLAN_SF_INDEX = 0;
    private int MPLS_SFF_INDEX = 0;

    SfcOfProviderUtilsTestMock sfcUtilsTestMock;

    private boolean usesLogicalSff = false;

    public RspBuilder(SfcOfProviderUtilsTestMock sfcUtilsTestMock) {
        this.sfcUtilsTestMock = sfcUtilsTestMock;
    }

    public RenderedServicePath createRspFromSfTypes(List<SftTypeName> sfTypes, boolean usesLogicalSff) {
        List<ServiceFunction> sfList = new ArrayList<>();
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();
        SffName logicalSff = new SffName("logical");
        this.usesLogicalSff = usesLogicalSff;

        // build the logical sff
        sffList.add(createServiceFunctionForwarder(logicalSff, null, Mac.class, usesLogicalSff));

        // build the SFs
        sfTypes.forEach(sftTypeName -> {
            SfName sfName = new SfName(SF_NAME_PREFIX + String.valueOf(SF_NAME_INDEX++));
            sfList.add(createServiceFunction(sfName, logicalSff, sftTypeName, Mac.class));
        });

        return createRsp(sfTypes, sfList, sffList, Mac.class, Nsh.class);
    }

    public RenderedServicePath createRspFromSfTypes(List<SftTypeName> sfTypes,
            Class<? extends SlTransportType> transportType, Class<? extends SfcEncapsulationIdentity> sfcEncap) {

        List<ServiceFunction> sfList = new ArrayList<>();
        List<ServiceFunctionForwarder> sffList = new ArrayList<>();

        for (SftTypeName sfType : sfTypes) {
            SfName sfName = new SfName(SF_NAME_PREFIX + String.valueOf(SF_NAME_INDEX++));
            SffName sffName = new SffName(SFF_NAME_PREFIX + String.valueOf(SFF_NAME_INDEX++));

            ServiceFunction sf = createServiceFunction(sfName, sffName, sfType, transportType);
            sfList.add(sf);
            sffList.add(createServiceFunctionForwarder(sffName, sf, transportType));
        }

        return createRsp(sfTypes, sfList, sffList, transportType, sfcEncap);
    }

    private RenderedServicePath createRsp(List<SftTypeName> sfTypes,
                                          List<ServiceFunction> sfList,
                                          List<ServiceFunctionForwarder> sffList,
                                          Class<? extends SlTransportType> transportType,
                                          Class<? extends SfcEncapsulationIdentity> sfcEncap) {
        ServiceFunctionChain sfc = createServiceFunctionChain(sfTypes);
        ServiceFunctionPath sfp = createServiceFunctionPath(sfc, transportType, sfcEncap);
        return createRenderedServicePath(sfp, sfList, sffList);
    }

    private ServiceFunctionChain createServiceFunctionChain(List<SftTypeName> sfTypes) {

        short order = 0;
        List<SfcServiceFunction> sfcSfs = new ArrayList<>();
        for (SftTypeName sfType : sfTypes) {
            String name = sfType.getValue() + "Abstract";
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            sfcSfBuilder.setKey(new SfcServiceFunctionKey(name));
            sfcSfBuilder.setName(name);
            sfcSfBuilder.setOrder(order++);
            sfcSfBuilder.setType(sfType);
            sfcSfs.add(sfcSfBuilder.build());
        }

        SfcName sfcName = new SfcName(SFC_NAME_PREFIX + String.valueOf(SFC_NAME_INDEX++));
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(sfcName);
        sfcBuilder.setKey(new ServiceFunctionChainKey(sfcName));
        sfcBuilder.setSymmetric(true);
        sfcBuilder.setSfcServiceFunction(sfcSfs);

        return sfcBuilder.build();
    }

    private ServiceFunctionPath createServiceFunctionPath(ServiceFunctionChain sfc,
            Class<? extends SlTransportType> transportType, Class <? extends SfcEncapsulationIdentity> encap) {

        SfpName sfpName = new SfpName(SFP_NAME_PREFIX + String.valueOf(SFP_NAME_INDEX++));
        ServiceFunctionPathBuilder sfpBuilder = new ServiceFunctionPathBuilder();
        sfpBuilder.setKey(new ServiceFunctionPathKey(sfpName));
        sfpBuilder.setName(sfpName);
        sfpBuilder.setServiceChainName(sfc.getName());
        sfpBuilder.setSymmetric(true);
        sfpBuilder.setTransportType(transportType);
        sfpBuilder.setSfcEncapsulation(encap);

        return sfpBuilder.build();
    }

    private RenderedServicePath createRenderedServicePath(ServiceFunctionPath sfp, List<ServiceFunction> sfList,
            List<ServiceFunctionForwarder> sffList) {
        RspName rspName = new RspName(RSP_NAME_PREFIX + String.valueOf(RSP_NAME_INDEX++));
        RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setKey(new RenderedServicePathKey(rspName));
        rspBuilder.setStartingIndex((short) 255);
        rspBuilder.setName(rspName);
        rspBuilder.setParentServiceFunctionPath(sfp.getName());
        rspBuilder.setPathId(RSP_PATHID_INDEX++);
        rspBuilder.setTransportType(sfp.getTransportType());
        rspBuilder.setSfcEncapsulation(sfp.getSfcEncapsulation());

        short index = 0;
        short serviceIndex = 255;
        List<RenderedServicePathHop> rspHopList = new ArrayList<>();
        for (ServiceFunction sf : sfList) {
            ServiceFunctionForwarder sff = sffList.get(usesLogicalSff ? 0 : index);
            RenderedServicePathHopBuilder rspHopBuilder = new RenderedServicePathHopBuilder();
            rspHopBuilder.setKey(new RenderedServicePathHopKey(index));
            rspHopBuilder.setServiceFunctionForwarder(sff.getName());
            rspHopBuilder.setServiceFunctionName(sf.getName());
            // if we're using a logical SFF, there won't be any dataplane locators
            rspHopBuilder.setServiceFunctionForwarderLocator(
                    !sff.getSffDataPlaneLocator().isEmpty() ?
                                sff.getSffDataPlaneLocator().get(0).getName() : new SffDataPlaneLocatorName(""));
            rspHopBuilder.setServiceIndex(serviceIndex);
            rspHopBuilder.setHopNumber(index);

            rspHopList.add(rspHopBuilder.build());

            --serviceIndex;
            ++index;
        }

        rspBuilder.setRenderedServicePathHop(rspHopList);
        return rspBuilder.build();
    }

    private ServiceFunctionForwarder createServiceFunctionForwarder(
            SffName sffName, ServiceFunction sf, Class<? extends SlTransportType> transportType, boolean isLogicalSff) {
        if (!isLogicalSff) {
            return createServiceFunctionForwarder(sffName, sf, transportType);
        }
        else { // *do not set* the SFF dataplane locators
            ServiceFunctionForwarder sff =
                    buildServiceFunctionForwarder(
                            new SffName("logical"), Collections.emptyList(), Collections.emptyList());
            sfcUtilsTestMock.addServiceFunctionForwarder(sffName, sff);
            return sff;
        }
    }

    private ServiceFunctionForwarder createServiceFunctionForwarder(SffName sffName, ServiceFunction sf,
            Class<? extends SlTransportType> transportType) {
        List<SffDataPlaneLocator> sffDpls = createSffDpls(transportType);
        List<ServiceFunctionDictionary> sfDictList = createSfDictList(sf, sffDpls.get(0).getName());

        ServiceFunctionForwarder sff = buildServiceFunctionForwarder(sffName, sffDpls, sfDictList);
        sfcUtilsTestMock.addServiceFunctionForwarder(sffName, sff);

        return sff;
    }

    private ServiceFunction createServiceFunction(SfName sfName, SffName sffName, SftTypeName sfType,
            Class<? extends SlTransportType> transportType) {

        // For MPLS and MAC transport types, we want the SF to be MAC/VLAN
        Class<? extends SlTransportType> sfTransportType =
                (transportType.equals(VxlanGpe.class) ? transportType : Mac.class);

        SfDataPlaneLocator sfDpl = buildSfDataPlaneLocator(new SfDataPlaneLocatorName(sfName.getValue() + "_sfDpl"),
                buildSfLocatorType(sfTransportType), sffName, sfTransportType);

        ServiceFunction sf = buildServiceFunction(sfName, sfType, getIpMgmt(), sfDpl, false);
        sfcUtilsTestMock.addServiceFunction(sfName, sf);

        return sf;
    }

    private List<SffDataPlaneLocator> createSffDpls(Class<? extends SlTransportType> transportType) {
        int dplCount = (transportType.equals(VxlanGpe.class) ? 1 : 2);

        ArrayList<Integer> tunnelIds = new ArrayList<>();
        int tunnelIdsIndex = 0;
        // This allows us to have adjacent SFFs with matching tunnel info
        if (transportType.equals(Mac.class)) {
            tunnelIds.add(0, getLastSffVlanId());
            tunnelIds.add(1, getNextSffVlanId());
        } else if (transportType.equals(Mpls.class)) {
            tunnelIds.add(0, getLastSffMplsLabel());
            tunnelIds.add(1, getNextSffMplsLabel());
        }

        List<SffDataPlaneLocator> sffDpls = new ArrayList<>();
        for (int i = 0; i < dplCount; ++i) {
            SffDataPlaneLocatorName name =
                    new SffDataPlaneLocatorName(SFF_DPL_NAME_PREFIX + String.valueOf(SFF_DPL_NAME_INDEX++));
            SffDataPlaneLocatorBuilder sffDplBuilder = new SffDataPlaneLocatorBuilder();
            sffDplBuilder.setKey(new SffDataPlaneLocatorKey(name));
            sffDplBuilder.setName(name);
            DataPlaneLocatorBuilder dplBuilder = new DataPlaneLocatorBuilder();
            // check the transport type to see what type of locator to build
            if (transportType.equals(Mac.class)) {
                dplBuilder.setLocatorType(buildLocatorTypeMac(getNextMacAddress(), tunnelIds.get(tunnelIdsIndex++)));
            } else if (transportType.equals(Mpls.class)) {
                dplBuilder.setLocatorType(buildLocatorTypeMpls(tunnelIds.get(tunnelIdsIndex++)));
            } else if (transportType.equals(VxlanGpe.class)) {
                dplBuilder.setLocatorType(buildLocatorTypeIp(getNextIpAddress(), VXLAN_UDP_PORT));
            }
            dplBuilder.setTransport(transportType);
            sffDplBuilder.setDataPlaneLocator(dplBuilder.build());

            // Augment the SFF DPL if its not VxLan
            if (!transportType.equals(VxlanGpe.class)) {

                SffDataPlaneLocator1Builder ofsSffDplBuilder = new SffDataPlaneLocator1Builder();
                OfsPortBuilder ofsPortBuilder = new OfsPortBuilder();
                ofsPortBuilder.setMacAddress(new MacAddress(getNextMacAddress()));
                ofsPortBuilder.setPortId(SWITCH_PORT_STR);
                ofsSffDplBuilder.setOfsPort(ofsPortBuilder.build());
                sffDplBuilder.addAugmentation(SffDataPlaneLocator1.class, ofsSffDplBuilder.build());
            }

            sffDpls.add(sffDplBuilder.build());
        }

        return sffDpls;
    }

    private List<ServiceFunctionDictionary> createSfDictList(ServiceFunction sf, SffDataPlaneLocatorName sffDplName) {
        SffSfDataPlaneLocatorBuilder sffSfDplBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDplBuilder.setSfDplName(sf.getSfDataPlaneLocator().get(0).getName());    // TODO what if there is more than one DPL?
        sffSfDplBuilder.setSffDplName(sffDplName);

        ServiceFunctionDictionaryBuilder sfDictBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictBuilder.setName(sf.getName());
        sfDictBuilder.setSffSfDataPlaneLocator(sffSfDplBuilder.build());
        sfDictBuilder.setKey(new ServiceFunctionDictionaryKey(sf.getName()));

        List<ServiceFunctionDictionary> sfDictList = new ArrayList<>();
        sfDictList.add(sfDictBuilder.build());

        return sfDictList;
    }

    //
    // Internal Util methods
    //
    private String getNextMacAddress() {
        return MAC_ADDRESS_PREFIX + String.format("%02d", MAC_ADDR_INDEX++);
    }

    private int getNextSffVlanId() {
        return VLAN_BASE + (++VLAN_SFF_INDEX);
    }

    private int getLastSffVlanId() {
        return VLAN_BASE + VLAN_SFF_INDEX;
    }

    private int getNextSfVlanId() {
        return VLAN_BASE + (++VLAN_SF_INDEX);
    }

    private int getNextSffMplsLabel() {
        return MPLS_BASE + (++MPLS_SFF_INDEX);
    }

    private int getLastSffMplsLabel() {
        return MPLS_BASE + MPLS_SFF_INDEX;
    }

    private String getNextIpAddress() {
        return IP_ADDRESS_PREFIX + String.valueOf(IP_ADDR_INDEX++);
    }

    private IpAddress getIpMgmt() {
        // For now, using the same Mgmt address everywhere
        return new IpAddress(new Ipv4Address("10.0.0.1"));
    }

    private String getNextLogicalInterfaceName() {
        SecureRandom random = new SecureRandom();
        String seed = new BigInteger(130, random).toString(16);
        return "tap" + String.format("%s-%s", seed.substring(0,7), seed.substring(10,12));
    }

    private LocatorType buildSfLocatorType(Class<? extends SlTransportType> transport) {
        LocatorType lt = null;

        if (transport.equals(Mac.class)) {
            if (!usesLogicalSff) {
                lt = buildLocatorTypeMac(getNextMacAddress(), getNextSfVlanId());
                } else {
                lt = new LogicalInterfaceBuilder().setInterfaceName(getNextLogicalInterfaceName()).build();
            }
        } else if (transport.equals(Mpls.class)) {
            // NOTICE for now, we're not dealing with MPLS SFs
            lt = buildLocatorTypeMac(getNextMacAddress(), getNextSfVlanId());
        } else if (transport.equals(VxlanGpe.class)) {
            lt = buildLocatorTypeIp(getNextIpAddress(), VXLAN_UDP_PORT);
        }

        return lt;
    }

    private org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac buildLocatorTypeMac(
            String macAddress, int vlan) {
        MacBuilder macBuilder = new MacBuilder();
        macBuilder.setMac(new MacAddress(macAddress));
        macBuilder.setVlanId(vlan);

        return macBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls buildLocatorTypeMpls(
            long label) {
        MplsBuilder mplsBuilder = new MplsBuilder();
        mplsBuilder.setMplsLabel(label);

        return mplsBuilder.build();
    }

    private Ip buildLocatorTypeIp(String ipAddressStr, int port) {
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(new IpAddress(new Ipv4Address(ipAddressStr)));
        ipBuilder.setPort(new PortNumber(port));

        return ipBuilder.build();
    }

    //
    // Builder methods copied from SfcProvider SimpleTestEntityBuilder
    //

    private SfDataPlaneLocator buildSfDataPlaneLocator(SfDataPlaneLocatorName name, LocatorType locatorType,
            SffName serviceFunctionForwarder, Class<? extends SlTransportType> transport) {

        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder.setName(name)
            .setKey(new SfDataPlaneLocatorKey(name))
            .setLocatorType(locatorType)
            .setServiceFunctionForwarder(serviceFunctionForwarder)
            .setTransport(transport);

        return locatorBuilder.build();
    }

    private ServiceFunction buildServiceFunction(SfName name, SftTypeName type, IpAddress ipMgmtAddress,
            SfDataPlaneLocator sfDataPlaneLocator, Boolean nshAware) {

        List<SfDataPlaneLocator> dsfDataPlaneLocatorList = new ArrayList<>();
        dsfDataPlaneLocatorList.add(sfDataPlaneLocator);

        return buildServiceFunction(name, type, ipMgmtAddress, dsfDataPlaneLocatorList, nshAware);
    }

    private ServiceFunction buildServiceFunction(SfName name, SftTypeName type, IpAddress ipMgmtAddress,
            List<SfDataPlaneLocator> dsfDataPlaneLocatorList, Boolean nshAware) {

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setName(name)
            .setKey(new ServiceFunctionKey(name))
            .setType(type)
            .setIpMgmtAddress(ipMgmtAddress)
            .setSfDataPlaneLocator(dsfDataPlaneLocatorList)
            .setNshAware(nshAware);

        return sfBuilder.build();
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarder(SffName name,
            List<SffDataPlaneLocator> sffDataplaneLocatorList, List<ServiceFunctionDictionary> dictionaryList) {
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setName(name)
            .setKey(new ServiceFunctionForwarderKey(name))
            .setServiceNode(new SnName(name.getValue()))
            .setSffDataPlaneLocator(sffDataplaneLocatorList)
            .setServiceFunctionDictionary(dictionaryList);

        return sffBuilder.build();
    }
}
