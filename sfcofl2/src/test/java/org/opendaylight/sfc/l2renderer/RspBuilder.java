package org.opendaylight.sfc.l2renderer;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.info.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.info.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.info.RenderedServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.MplsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.ServiceFunctionDictionary1Builder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.ServiceFunctionDictionary1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1Builder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPortBuilder;

public class RspBuilder {

    private static String SF_NAME_PREFIX  = "SF_";
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

    SfcL2ProviderUtilsTestMock sfcUtilsTestMock;

    public RspBuilder(SfcL2ProviderUtilsTestMock sfcUtilsTestMock) {
        this.sfcUtilsTestMock = sfcUtilsTestMock;
    }

    public RenderedServicePath createRspFromSfTypes(
            List<Class<? extends ServiceFunctionTypeIdentity>> sfTypes,
            Class<? extends SlTransportType> transportType) {

        List<ServiceFunction> sfList = new ArrayList<ServiceFunction>();
        List<ServiceFunctionForwarder> sffList = new ArrayList<ServiceFunctionForwarder>();

        for(Class<? extends ServiceFunctionTypeIdentity> sfType : sfTypes) {
            String sfName = SF_NAME_PREFIX + String.valueOf(SF_NAME_INDEX++);
            String sffName = SFF_NAME_PREFIX + String.valueOf(SFF_NAME_INDEX++);

            ServiceFunction sf = createServiceFunction(sfName, sffName, sfType, transportType);
            sfList.add(sf);
            sffList.add(createServiceFunctionForwarder(sffName, sf, transportType));
        }

        ServiceFunctionChain sfc = createServiceFunctionChain(sfTypes);
        ServiceFunctionPath sfp = createServiceFunctionPath(sfc, transportType);
        RenderedServicePath rsp = createRenderedServicePath(sfp, sfList, sffList);

        return rsp;
    }

    public ServiceFunctionChain createServiceFunctionChain(
            List<Class<? extends ServiceFunctionTypeIdentity>> sfTypes) {

        short order = 0;
        List<SfcServiceFunction> sfcSfs = new ArrayList<SfcServiceFunction>();
        for(Class<? extends ServiceFunctionTypeIdentity> sfType : sfTypes) {
            String name = sfType.getName() + "Abstract";
            SfcServiceFunctionBuilder sfcSfBuilder = new SfcServiceFunctionBuilder();
            sfcSfBuilder.setKey(new SfcServiceFunctionKey(name));
            sfcSfBuilder.setName(name);
            sfcSfBuilder.setOrder(order++);
            sfcSfBuilder.setType(sfType);
            sfcSfs.add(sfcSfBuilder.build());
        }

        String sfcName = SFC_NAME_PREFIX + String.valueOf(SFC_NAME_INDEX++);
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();
        sfcBuilder.setName(sfcName);
        sfcBuilder.setKey(new ServiceFunctionChainKey(sfcName));
        sfcBuilder.setSymmetric(true);
        sfcBuilder.setSfcServiceFunction(sfcSfs);

        return sfcBuilder.build();
    }

    public ServiceFunctionPath createServiceFunctionPath(
            ServiceFunctionChain sfc,
            Class<? extends SlTransportType> transportType) {

        String sfpName = SFP_NAME_PREFIX + String.valueOf(SFP_NAME_INDEX++);
        ServiceFunctionPathBuilder sfpBuilder = new ServiceFunctionPathBuilder();
        sfpBuilder.setKey(new ServiceFunctionPathKey(sfpName));
        sfpBuilder.setName(sfpName);
        sfpBuilder.setServiceChainName(sfc.getName());
        sfpBuilder.setSymmetric(true);
        sfpBuilder.setTransportType(transportType);

        return sfpBuilder.build();
    }

    public RenderedServicePath createRenderedServicePath(
            ServiceFunctionPath sfp, List<ServiceFunction> sfList, List<ServiceFunctionForwarder> sffList) {
        String rspName = RSP_NAME_PREFIX + String.valueOf(RSP_NAME_INDEX++);
        RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setKey(new RenderedServicePathKey(rspName));
        rspBuilder.setStartingIndex(new Short((short) 255));
        rspBuilder.setName(rspName);
        rspBuilder.setParentServiceFunctionPath(sfp.getName());
        rspBuilder.setPathId(RSP_PATHID_INDEX++);
        rspBuilder.setTransportType(sfp.getTransportType());

        short index = 0;
        short serviceIndex = 255;
        List<RenderedServicePathHop> rspHopList = new ArrayList<RenderedServicePathHop>();
        for(ServiceFunction sf : sfList) {
            ServiceFunctionForwarder sff = sffList.get(index);
            RenderedServicePathHopBuilder rspHopBuilder = new RenderedServicePathHopBuilder();
            rspHopBuilder.setKey(new RenderedServicePathHopKey(index));
            rspHopBuilder.setServiceFunctionForwarder(sff.getName());
            rspHopBuilder.setServiceFunctionName(sf.getName());
            rspHopBuilder.setServiceFunctionForwarderLocator(sff.getSffDataPlaneLocator().get(0).getName());
            rspHopBuilder.setServiceIndex(serviceIndex);
            rspHopBuilder.setHopNumber(index);

            rspHopList.add(rspHopBuilder.build());

            --serviceIndex;
            ++index;
        }

        rspBuilder.setRenderedServicePathHop(rspHopList);
        return rspBuilder.build();
    }

    public ServiceFunctionForwarder createServiceFunctionForwarder(
            String sffName,
            ServiceFunction sf,
            Class<? extends SlTransportType> transportType) {
        List<SffDataPlaneLocator> sffDpls = createSffDpls(transportType);
        List<ServiceFunctionDictionary> sfDictList = createSfDictList(sf, transportType);

        ServiceFunctionForwarder sff = buildServiceFunctionForwarder(sffName, sffDpls, sfDictList, "");
        sfcUtilsTestMock.addServiceFunctionForwarder(sffName, sff);

        return sff;
    }

    public ServiceFunction createServiceFunction(
            String sfName,
            String sffName,
            Class<? extends ServiceFunctionTypeIdentity> sfType,
            Class<? extends SlTransportType> transportType) {

        // For MPLS and MAC transport types, we want the SF to be MAC/VLAN
        Class<? extends SlTransportType> sfTransportType =
                (transportType.equals(VxlanGpe.class) ? transportType : Mac.class);

        SfDataPlaneLocator sfDpl = buildSfDataPlaneLocator(
                sfName+"_sfDpl", buildSfLocatorType(sfTransportType), sffName, sfTransportType);

        ServiceFunction sf = buildServiceFunction(sfName, sfType, getIpMgmt(), sfDpl, false);
        sfcUtilsTestMock.addServiceFunction(sfName, sf);

        return sf;
    }

    private List<SffDataPlaneLocator> createSffDpls(Class<? extends SlTransportType> transportType) {
        int dplCount = (transportType.equals(VxlanGpe.class) ? 1 : 2);

        ArrayList<Integer> tunnelIds = new ArrayList<Integer>();
        int tunnelIdsIndex = 0;
        // This allows us to have adjacent SFFs with matching tunnel info
        if(transportType.equals(Mac.class)) {
            tunnelIds.add(0, Integer.valueOf(getLastSffVlanId()));
            tunnelIds.add(1, Integer.valueOf(getNextSffVlanId()));
        } else if(transportType.equals(Mpls.class)) {
            tunnelIds.add(0, Integer.valueOf(getLastSffMplsLabel()));
            tunnelIds.add(1, Integer.valueOf(getNextSffMplsLabel()));
        }

        List<SffDataPlaneLocator> sffDpls = new ArrayList<SffDataPlaneLocator>();
        for(int i = 0; i < dplCount; ++i) {
            String name = SFF_DPL_NAME_PREFIX + String.valueOf(SFF_DPL_NAME_INDEX++);
            SffDataPlaneLocatorBuilder sffDplBuilder = new SffDataPlaneLocatorBuilder();
            sffDplBuilder.setKey(new SffDataPlaneLocatorKey(name));
            sffDplBuilder.setName(name);
            DataPlaneLocatorBuilder dplBuilder = new DataPlaneLocatorBuilder();
            // check the transport type to see what type of locator to build
            if(transportType.equals(Mac.class)) {
                dplBuilder.setLocatorType(buildLocatorTypeMac(getNextMacAddress(), tunnelIds.get(tunnelIdsIndex++)));
            } else if(transportType.equals(Mpls.class)) {
                dplBuilder.setLocatorType(buildLocatorTypeMpls(tunnelIds.get(tunnelIdsIndex++)));
            } else if(transportType.equals(VxlanGpe.class)) {
                dplBuilder.setLocatorType(buildLocatorTypeIp(getNextIpAddress(), VXLAN_UDP_PORT));
            }
            dplBuilder.setTransport(transportType);
            sffDplBuilder.setDataPlaneLocator(dplBuilder.build());

            // Augment the SFF DPL if its not VxLan
            if(!transportType.equals(VxlanGpe.class)) {

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

    private List<ServiceFunctionDictionary> createSfDictList(ServiceFunction sf, Class<? extends SlTransportType> transportType) {
        // For MPLS and MAC transport types, we want the SF to be MAC/VLAN
        Class<? extends SlTransportType> sfTransportType =
                (transportType.equals(VxlanGpe.class) ? transportType : Mac.class);

        SffSfDataPlaneLocatorBuilder sffSfDplBuilder = new SffSfDataPlaneLocatorBuilder();
        // TODO the vlanId needs to be the same as on the SF
        sffSfDplBuilder.setLocatorType(buildSfLocatorType(sfTransportType));
        sffSfDplBuilder.setTransport(sfTransportType);

        ServiceFunctionDictionaryBuilder sfDictBuilder = new ServiceFunctionDictionaryBuilder();
        sfDictBuilder.setName(sf.getName());
        sfDictBuilder.setType(sf.getType());
        sfDictBuilder.setSffSfDataPlaneLocator(sffSfDplBuilder.build());
        sfDictBuilder.setKey(new ServiceFunctionDictionaryKey(sf.getName()));

        // Augment the dictionary with an OfsPortBuilder if its not VxLan
        if(!transportType.equals(VxlanGpe.class)) {
            ServiceFunctionDictionary1Builder ofsSfDictBuilder = new ServiceFunctionDictionary1Builder();
            OfsPortBuilder ofsPortBuilder = new OfsPortBuilder();
            ofsPortBuilder.setMacAddress(new MacAddress(getNextMacAddress()));
            ofsPortBuilder.setPortId(SWITCH_PORT_STR);
            ofsSfDictBuilder.setOfsPort(ofsPortBuilder.build());
            sfDictBuilder.addAugmentation(ServiceFunctionDictionary1.class, ofsSfDictBuilder.build());
        }

        List<ServiceFunctionDictionary> sfDictList = new ArrayList<ServiceFunctionDictionary>();
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

    private LocatorType buildSfLocatorType(Class<? extends SlTransportType> transport) {
        LocatorType lt = null;

        if(transport.equals(Mac.class)) {
            lt = buildLocatorTypeMac(getNextMacAddress(), getNextSfVlanId());
        }
        else if(transport.equals(Mpls.class)) {
            // NOTICE for now, we're not dealing with MPLS SFs
            lt = buildLocatorTypeMac(getNextMacAddress(), getNextSfVlanId());
        }
        else if(transport.equals(VxlanGpe.class)) {
            lt = buildLocatorTypeIp(getNextIpAddress(), VXLAN_UDP_PORT);
        }

        return lt;
    }

    private org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac
    buildLocatorTypeMac(String macAddress, int vlan) {
        MacBuilder macBuilder = new MacBuilder();
        macBuilder.setMac(new MacAddress(macAddress));
        macBuilder.setVlanId(vlan);

        return macBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mpls
    buildLocatorTypeMpls(long label) {
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

    private SfDataPlaneLocator buildSfDataPlaneLocator(
            String name, LocatorType locatorType, String serviseFunctionForwarder, Class<? extends SlTransportType> transport) {

        SfDataPlaneLocatorBuilder locatorBuilder = new SfDataPlaneLocatorBuilder();
        locatorBuilder
            .setName(name)
            .setKey(new SfDataPlaneLocatorKey(name))
            .setLocatorType(locatorType)
            .setServiceFunctionForwarder(serviseFunctionForwarder)
            .setTransport(transport);

        return locatorBuilder.build();
    }

    private ServiceFunction buildServiceFunction(
            String name, Class<? extends ServiceFunctionTypeIdentity> type, IpAddress ipMgmtAddress, SfDataPlaneLocator sfDataPlaneLocator, Boolean nshAware) {

        List<SfDataPlaneLocator> dsfDataPlaneLocatorList = new ArrayList<>();
        dsfDataPlaneLocatorList.add(sfDataPlaneLocator);

        return buildServiceFunction(name, type, ipMgmtAddress, dsfDataPlaneLocatorList, nshAware);
    }

    private ServiceFunction buildServiceFunction(
            String name, Class<? extends ServiceFunctionTypeIdentity> type, IpAddress ipMgmtAddress, List<SfDataPlaneLocator> dsfDataPlaneLocatorList, Boolean nshAware) {

        ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder
            .setName(name)
            .setKey(new ServiceFunctionKey(name))
            .setType(type)
            .setIpMgmtAddress(ipMgmtAddress)
            .setSfDataPlaneLocator(dsfDataPlaneLocatorList)
            .setNshAware(nshAware);

        return sfBuilder.build();
    }

    private ServiceFunctionForwarder buildServiceFunctionForwarder(
            String name, List<SffDataPlaneLocator> sffDataplaneLocatorList, List<ServiceFunctionDictionary> dictionaryList, String classifier) {
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder
            .setName(name)
            .setKey(new ServiceFunctionForwarderKey(name))
            .setServiceNode(name)
            .setSffDataPlaneLocator(sffDataplaneLocatorList)
            .setServiceFunctionDictionary(dictionaryList);

        return sffBuilder.build();
    }
}
