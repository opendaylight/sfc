package org.opendaylight.sfc.provider;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.lispflowmapping.interfaces.lisp.IFlowMapping;
import org.opendaylight.lispflowmapping.type.AddressFamilyNumberEnum;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.LispAFIAddress;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.MapReply;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.MapRequest;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.eidrecords.EidRecord;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.eidrecords.EidRecordBuilder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.eidtolocatorrecords.EidToLocatorRecord;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.LispAddressContainer;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.LispAddressContainerBuilder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.Address;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.DistinguishedName;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.DistinguishedNameBuilder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.Ipv4;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.LcafApplicationData;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.lispaddress.lispaddresscontainer.address.NoBuilder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.locatorrecords.LocatorRecord;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.maprequest.ItrRloc;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.maprequest.ItrRlocBuilder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.maprequest.SourceEidBuilder;
import org.opendaylight.yang.gen.v1.lispflowmapping.rev131031.maprequestnotification.MapRequestBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

import com.google.common.net.InetAddresses;

public class LispUpdater {

    private IFlowMapping flowMapping;

    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    public LispUpdater() {
    }

    public LispAddressContainer asLispContainer(String rloc) {
        LispAddressContainerBuilder builder = new LispAddressContainerBuilder();
        builder.setAddress(buildDistinguishedNameAddress(rloc));
        return builder.build();
    }

    private DistinguishedName buildDistinguishedNameAddress(String rloc) {
        return new DistinguishedNameBuilder().setAfi(AddressFamilyNumberEnum.DISTINGUISHED_NAME.getIanaCode()).setDistinguishedName(rloc).build();
    }

    // @Override
    // public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>,
    // DataObject> change) {
    //
    // // SF CREATION
    // Map<InstanceIdentifier<?>, DataObject> dataCreatedObject =
    // change.getCreatedData();
    // InstanceIdentifier<ServiceFunctionForwarders> sffsIID;
    // sffsIID =
    // InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();
    //
    // /*
    // * when a SFF is created we will process it and add lisp data
    // */
    // for (Map.Entry<InstanceIdentifier<?>, DataObject> entry :
    // dataCreatedObject.entrySet()) {
    // if (entry.getValue() instanceof ServiceFunctionForwarders) {
    //
    // ServiceFunctionForwarders createdServiceFunctionForwarders =
    // (ServiceFunctionForwarders) entry.getValue();
    // List<ServiceFunctionForwarder> serviceFunctionForwarderList =
    // createdServiceFunctionForwarders.getServiceFunctionForwarder();
    // List<ServiceFunctionForwarder> newServiceFunctionForwarderList = new
    // ArrayList<>();
    // for (ServiceFunctionForwarder serviceFunctionForwarder :
    // serviceFunctionForwarderList) {
    // List<SffDataPlaneLocator> locations =
    // serviceFunctionForwarder.getSffDataPlaneLocator();
    // Lisp lispLocation = null;
    // for (SffDataPlaneLocator location : locations) {
    // DataPlaneLocator dpl = location.getDataPlaneLocator();
    // LocatorType lt = dpl.getLocatorType();
    // if (lt instanceof Lisp) {
    // lispLocation = (Lisp) lt;
    // }
    // }
    // if (lispLocation != null) {
    // ServiceFunctionForwarder updatedForwarder = updateLispData(lispLocation,
    // serviceFunctionForwarder);
    // newServiceFunctionForwarderList.add(updatedForwarder);
    // } else {
    // newServiceFunctionForwarderList.add(serviceFunctionForwarder);
    // }
    // }
    // WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
    // ServiceFunctionForwarders serviceFunctionForwarders = new
    // ServiceFunctionForwardersBuilder(createdServiceFunctionForwarders)
    // .setServiceFunctionForwarder(newServiceFunctionForwarderList).build();
    // writeTx.merge(LogicalDatastoreType.CONFIGURATION, sffsIID,
    // serviceFunctionForwarders, true);
    // writeTx.commit();
    // }
    // }
    //
    // }

    public ServiceFunctionForwarder updateLispData(ServiceFunctionForwarder serviceFunctionForwarder) {
        List<SffDataPlaneLocator> locations = serviceFunctionForwarder.getSffDataPlaneLocator();
        Lisp lispLocation = null;
        for (SffDataPlaneLocator location : locations) {
            DataPlaneLocator dpl = location.getDataPlaneLocator();
            LocatorType lt = dpl.getLocatorType();
            if (lt instanceof Lisp) {
                lispLocation = (Lisp) lt;
            }
        }
        if (lispLocation != null) {
            return updateLispData(lispLocation, serviceFunctionForwarder);
        } else {
            return serviceFunctionForwarder;
        }
    }

    public ServiceFunction updateLispData(ServiceFunction serviceFunction) {
        List<SfDataPlaneLocator> locations = serviceFunction.getSfDataPlaneLocator();
        Lisp lispLocation = null;
        for (SfDataPlaneLocator location : locations) {
            LocatorType lt = location.getLocatorType();
            if (lt instanceof Lisp) {
                lispLocation = (Lisp) lt;
            }
        }
        if (lispLocation != null) {
            return updateLispData(lispLocation, serviceFunction);
        } else {
            return serviceFunction;
        }
    }

    private ServiceFunction updateLispData(Lisp lispLocation, ServiceFunction serviceFunction) {
        MapRequest mr = createMapRequest(lispLocation.getIp());
        MapReply reply = flowMapping.handleMapRequest(mr);
        if (reply.getEidToLocatorRecord() == null || reply.getEidToLocatorRecord().isEmpty()) {
            return serviceFunction;
        }
        EidToLocatorRecord etlr = reply.getEidToLocatorRecord().get(0);
        for (LocatorRecord locator : etlr.getLocatorRecord()) {
            Address address = locator.getLispAddressContainer().getAddress();
            if (address instanceof LcafApplicationData) {
                LcafApplicationData applicationData = (LcafApplicationData) address;
                Ip locatorType = createLocator(applicationData);
                String name = lispLocation.getIp().toString();
                SfDataPlaneLocatorKey key = new SfDataPlaneLocatorKey(name);
                SfDataPlaneLocator loc = new SfDataPlaneLocatorBuilder().setLocatorType(locatorType).setKey(key).setName(name).build();
                ServiceFunctionBuilder fb = new ServiceFunctionBuilder(serviceFunction);
                fb.getSfDataPlaneLocator().add(loc);
                return fb.build();
            }

        }
        return serviceFunction;

    }

    private ServiceFunctionForwarder updateLispData(Lisp lispLocation, ServiceFunctionForwarder serviceFunctionForwarder) {
        MapRequest mr = createMapRequest(lispLocation.getIp());
        MapReply reply = flowMapping.handleMapRequest(mr);
        if (reply.getEidToLocatorRecord() == null || reply.getEidToLocatorRecord().isEmpty()) {
            return serviceFunctionForwarder;
        }
        EidToLocatorRecord etlr = reply.getEidToLocatorRecord().get(0);
        for (LocatorRecord locator : etlr.getLocatorRecord()) {
            Address address = locator.getLispAddressContainer().getAddress();
            if (address instanceof LcafApplicationData) {
                LcafApplicationData applicationData = (LcafApplicationData) address;
                Ip locatorType = createLocator(applicationData);
                DataPlaneLocator dpl = new DataPlaneLocatorBuilder().setLocatorType(locatorType).build();
                String name = lispLocation.getIp().toString();
                SffDataPlaneLocatorKey key = new SffDataPlaneLocatorKey(name);
                SffDataPlaneLocator loc = new SffDataPlaneLocatorBuilder().setDataPlaneLocator(dpl).setKey(key).setName(name).build();
                ServiceFunctionForwarderBuilder fb = new ServiceFunctionForwarderBuilder(serviceFunctionForwarder);
                fb.getSffDataPlaneLocator().add(loc);
                return fb.build();
            }

        }
        return serviceFunctionForwarder;

    }

    private Ip createLocator(LcafApplicationData applicationData) {
        IpAddress ip = new IpAddress(new Ipv4Address(InetAddresses.fromInteger(applicationData.getIpTos()).getHostAddress()));
        Ip locatorType = new IpBuilder().setIp(ip).setPort(applicationData.getLocalPort()).build();
        return locatorType;
    }

    public MapRequest createMapRequest(IpAddress ip) {
        MapRequestBuilder mapRequestBuilder = new MapRequestBuilder();
        mapRequestBuilder.setNonce((long) 4);
        mapRequestBuilder.setPitr(false);
        mapRequestBuilder
                .setSourceEid(new SourceEidBuilder().setLispAddressContainer(toContainer(new NoBuilder().setAfi((short) 0).build())).build());
        mapRequestBuilder.setEidRecord(new ArrayList<EidRecord>());
        mapRequestBuilder.getEidRecord().add(new EidRecordBuilder().setMask((short) 32).setLispAddressContainer(asLispContainer(ip)).build());
        mapRequestBuilder.setItrRloc(new ArrayList<ItrRloc>());
        mapRequestBuilder.getItrRloc().add(new ItrRlocBuilder().setLispAddressContainer(toContainer(asIPAfiAddress("127.0.0.1"))).build());
        MapRequest mr = mapRequestBuilder.build();
        return mr;
    }

    public LispAddressContainer asLispContainer(IpAddress eid) {
        LispAddressContainerBuilder builder = new LispAddressContainerBuilder();
        builder.setAddress(asLispAddress(eid));
        return builder.build();
    }

    protected Address asLispAddress(IpAddress eid) {
        if (eid.getIpv4Address() != null) {
            return new Ipv4Builder().setAfi(AddressFamilyNumberEnum.IP.getIanaCode()).setIpv4Address(eid.getIpv4Address()).build();

        } else if (eid.getIpv6Address() != null) {
            return new Ipv6Builder().setAfi(AddressFamilyNumberEnum.IP6.getIanaCode()).setIpv6Address(eid.getIpv6Address()).build();
        }
        return null;
    }

    protected LispAddressContainer toContainer(LispAFIAddress address) {
        if (address instanceof Address) {
            return new LispAddressContainerBuilder().setAddress((Address) address).build();
        } else {
            return null;
        }
    }

    protected Ipv4 asIPAfiAddress(String ip) {
        return new Ipv4Builder().setIpv4Address(new Ipv4Address(ip)).setAfi((short) AddressFamilyNumberEnum.IP.getIanaCode()).build();
    }

}
