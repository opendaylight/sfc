/*
 * Copyright (c) 2015 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_lisp.provider;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.lispflowmapping.type.AddressFamilyNumberEnum;
import org.opendaylight.lispflowmapping.type.LispCanonicalAddressFormatEnum;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.LispAFIAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.MapRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.EidToLocatorRecord.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.eidrecords.EidRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.eidrecords.EidRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lcafsourcedestaddress.DstAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lcafsourcedestaddress.SrcAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lcaftrafficengineeringaddress.Hops;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lcaftrafficengineeringaddress.HopsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.LispAddressContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.LispAddressContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafApplicationData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafSourceDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafSourceDestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafTrafficEngineering;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafTrafficEngineeringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.ipv4.Ipv4AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.ipv6.Ipv6AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.lcafsourcedest.LcafSourceDestAddr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.lcafsourcedest.LcafSourceDestAddrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.lcaftrafficengineering.LcafTrafficEngineeringAddrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.no.NoAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.PrimitiveAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.locatorrecords.LocatorRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.locatorrecords.LocatorRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.maprequest.ItrRloc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.maprequest.ItrRlocBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.maprequest.SourceEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.maprequestnotification.MapRequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.reencaphop.HopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.AddMappingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.AddMappingInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.EidUri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.GetMappingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.GetMappingInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.MappingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.RemoveMappingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.RemoveMappingInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.db.instance.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.db.instance.MappingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class LispUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LispUtil.class);

    public static MapRequest createMapRequest(LispAddressContainer eidAddr) {
        MapRequestBuilder mapRequestBuilder = new MapRequestBuilder();
        mapRequestBuilder.setNonce((long) 4);
        mapRequestBuilder.setPitr(false);
        mapRequestBuilder.setSourceEid(new SourceEidBuilder().setLispAddressContainer(
                toContainer(new NoAddressBuilder().setAfi((short) 0).build())).build());
        mapRequestBuilder.setEidRecord(new ArrayList<EidRecord>());
        mapRequestBuilder.getEidRecord().add(
                new EidRecordBuilder().setMask((short) 32).setLispAddressContainer(eidAddr).build());
        mapRequestBuilder.setItrRloc(new ArrayList<ItrRloc>());
        mapRequestBuilder.getItrRloc().add(
                new ItrRlocBuilder().setLispAddressContainer(toContainer(ipStringToAfiAddress("127.0.0.1"))).build());
        MapRequest mr = mapRequestBuilder.build();
        return mr;
    }

    public static MapRequest createMapRequest(IpAddress ip) {
        return createMapRequest(toContainer(ip));
    }

    public static LispAddressContainer asLispContainer(Address addr) {
        LispAddressContainerBuilder builder = new LispAddressContainerBuilder();
        builder.setAddress(addr);
        return builder.build();
    }

    public static LispAddressContainer toContainer(IpAddress eid) {
        return asLispContainer(toLispAddress(eid));
    }

    public static Address toLispAddress(IpAddress eid) {
        if (eid.getIpv4Address() != null) {
            return (Address) new Ipv4AddressBuilder().setAfi(AddressFamilyNumberEnum.IP.getIanaCode())
                    .setIpv4Address(eid.getIpv4Address()).build();
        } else if (eid.getIpv6Address() != null) {
            return (Address) new Ipv6AddressBuilder().setAfi(AddressFamilyNumberEnum.IP6.getIanaCode())
                    .setIpv6Address(eid.getIpv6Address()).build();
        }
        return null;
    }

    public static LispAddressContainer toContainer(LispAFIAddress address) {
        if (address instanceof Address) {
            return new LispAddressContainerBuilder().setAddress((Address) address).build();
        } else {
            return null;
        }
    }

    public static Address toLispAddress(PrimitiveAddress addr) {
        if (addr instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv4) {
            return new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.Ipv4Builder()
                    .setIpv4Address(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.ipv4.Ipv4AddressBuilder()
                                    .setIpv4Address(
                                            ((org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv4) addr)
                                                    .getIpv4Address().getIpv4Address()).build()).build();
        } else if (addr instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv6) {
            return new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.Ipv6Builder()
                    .setIpv6Address(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.ipv6.Ipv6AddressBuilder()
                                    .setIpv6Address(
                                            ((org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv6) addr)
                                                    .getIpv6Address().getIpv6Address()).build()).build();
        } else {
            LOG.error("Conversion from {} to LispAddress not supported!", addr);
        }
        return null;
    }

    public static LispAddressContainer toContainer(PrimitiveAddress address) {
        return asLispContainer(toLispAddress(address));
    }

    public static LispAddressContainer ipStringToContainer(String ip) {
        return toContainer(ipStringToAfiAddress(ip));
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.LispAFIAddress ipStringToAfiAddress(
            String ip) {
        if (getIpAfiFromString(ip) == 1) {
            return new Ipv4AddressBuilder().setIpv4Address(new Ipv4Address(ip))
                    .setAfi((short) AddressFamilyNumberEnum.IP.getIanaCode()).build();
        } else {
            return new Ipv6AddressBuilder().setIpv6Address(new Ipv6Address(ip))
                    .setAfi((short) AddressFamilyNumberEnum.IP6.getIanaCode()).build();
        }
    }

    public static PrimitiveAddress toPrimitive(IpAddress ip) {
        if (ip.getIpv4Address() != null) {
            return new Ipv4Builder()
                    .setIpv4Address(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.ipv4.Ipv4AddressBuilder()
                                    .setIpv4Address(ip.getIpv4Address())
                                    .setAfi(AddressFamilyNumberEnum.IP.getIanaCode()).build()).build();
        } else if (ip.getIpv6Address() != null) {
            return new Ipv6Builder()
                    .setIpv6Address(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.ipv6.Ipv6AddressBuilder()
                                    .setIpv6Address(ip.getIpv6Address())
                                    .setAfi(AddressFamilyNumberEnum.IP.getIanaCode()).build()).build();
        } else {
            LOG.error("IP address not initialized {}", ip);
        }
        return null;
    }

    public static int getIpAfiFromString(String ip) {
        if (ip.indexOf(":") == -1) {
            return 1;
        } else {
            return 2;
        }
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.ipv4.Ipv4Address buildPrimitiveIpv4Address(
            String ip) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.ipv4.Ipv4AddressBuilder()
                .setAfi((short) AddressFamilyNumberEnum.IP.getIanaCode()).setIpv4Address(new Ipv4Address(ip)).build();
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.ipv6.Ipv6Address buildPrimitiveIpv6Address(
            String ip) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.ipv6.Ipv6AddressBuilder()
                .setAfi((short) AddressFamilyNumberEnum.IP6.getIanaCode()).setIpv6Address(new Ipv6Address(ip)).build();
    }

    public static PrimitiveAddress buildIpPrimiteAddress(String address) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.PrimitiveAddress addr;
        if (getIpAfiFromString(address) == 1) {
            addr = new Ipv4Builder().setIpv4Address(buildPrimitiveIpv4Address(address)).build();
        } else {
            addr = new Ipv6Builder().setIpv6Address(buildPrimitiveIpv6Address(address)).build();
        }
        return addr;
    }

    public static LcafSourceDest buildSrcDst(String address1, String address2, int mask1, int mask2) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.PrimitiveAddress addr1, addr2;
        addr1 = buildIpPrimiteAddress(address1);
        addr2 = buildIpPrimiteAddress(address2);

        LcafSourceDestAddr addr = new LcafSourceDestAddrBuilder().setAfi(AddressFamilyNumberEnum.LCAF.getIanaCode())
                .setLcafType((short) LispCanonicalAddressFormatEnum.SOURCE_DEST.getLispCode())
                .setSrcAddress(new SrcAddressBuilder().setPrimitiveAddress(addr1).build())
                .setSrcMaskLength((short) mask1)
                .setDstAddress(new DstAddressBuilder().setPrimitiveAddress(addr2).build())
                .setDstMaskLength((short) mask2).build();

        return new LcafSourceDestBuilder().setLcafSourceDestAddr(addr).build();
    }

    public static String containerToString(LispAddressContainer container) {
        Address address = container.getAddress();
        if (address instanceof LcafSourceDest) {
            return srcDstToString((LcafSourceDest) address);
        } else {
            LOG.warn("Converting address {} to string is not supported!", container);
        }
        return null;
    }

    public static String srcDstToString(LcafSourceDest sdAddr) {
        LcafSourceDestAddr addr = sdAddr.getLcafSourceDestAddr();
        return primitiveToString(addr.getSrcAddress().getPrimitiveAddress()) + "-" + addr.getSrcMaskLength() + "-"
                + primitiveToString(addr.getDstAddress().getPrimitiveAddress()) + "-" + addr.getDstMaskLength();
    }

    public static String primitiveToString(PrimitiveAddress primitive) {
        if (primitive instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv4) {
            return ((org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv4) primitive)
                    .getIpv4Address().getIpv4Address().getValue();
        }
        if (primitive instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv6) {
            return ((org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Ipv6) primitive)
                    .getIpv6Address().getIpv6Address().getValue();
        }
        if (primitive instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Mac) {
            return ((org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispsimpleaddress.primitiveaddress.Mac) primitive)
                    .getMacAddress().getMacAddress().getValue();
        }

        return null;
    }

    public static Mapping buildMapping(LispAddressContainer eid, List<LispAddressContainer> locators) {
        MappingBuilder mb = new MappingBuilder();
        mb.setEid(new EidUri(containerToString(eid)));
        mb.setOrigin(MappingOrigin.Northbound);
        mb.setRecordTtl(1440);
        mb.setMaskLength((short) 0);
        mb.setMapVersion((short) 0);
        mb.setAction(Action.NoAction);
        mb.setAuthoritative(true);
        mb.setLispAddressContainer(eid);
        mb.setLocatorRecord(buildLocatorRecords(locators));
        return mb.build();
    }

    public static List<LocatorRecord> buildLocatorRecords(List<LispAddressContainer> locators) {
        List<LocatorRecord> locatorRecords = new ArrayList<LocatorRecord>();
        for (LispAddressContainer locator : locators) {
            LocatorRecordBuilder locatorBuilder = new LocatorRecordBuilder();
            locatorBuilder.setLocalLocator(false).setRlocProbed(false).setWeight((short) 1).setPriority((short) 1)
                    .setMulticastWeight((short) 1).setMulticastPriority((short) 1).setRouted(true)
                    .setLispAddressContainer(locator).setName("SFC_LISP").build();
            locatorRecords.add(locatorBuilder.build());
        }
        return locatorRecords;
    }

    public static LcafTrafficEngineering buildTeLcaf(List<IpAddress> hopList) {
        LcafTrafficEngineeringBuilder teBuilder = new LcafTrafficEngineeringBuilder();
        LcafTrafficEngineeringAddrBuilder teAddrBuilder = new LcafTrafficEngineeringAddrBuilder();
        teAddrBuilder.setAfi(AddressFamilyNumberEnum.LCAF.getIanaCode()).setLcafType(
                (short) LispCanonicalAddressFormatEnum.TRAFFIC_ENGINEERING.getLispCode());
        teAddrBuilder.setHops(new ArrayList<Hops>());
        for (IpAddress hop : hopList) {
            HopBuilder hopBuilder = new HopBuilder();
            hopBuilder.setPrimitiveAddress(toPrimitive(hop));
            HopsBuilder hopsBuilder = new HopsBuilder();
            hopsBuilder.setName("Hop " + teAddrBuilder.getHops().size());
            hopsBuilder.setHop(hopBuilder.build());
            hopsBuilder.setLookup(false);
            hopsBuilder.setRLOCProbe(false);
            hopsBuilder.setStrict(false);
            teAddrBuilder.getHops().add(hopsBuilder.build());
        }

        teBuilder.setLcafTrafficEngineeringAddr(teAddrBuilder.build());
        return teBuilder.build();
    }

    public static Ip createLocator(LcafApplicationData applicationData) {
        IpAddress ip = new IpAddress(new Ipv4Address(InetAddresses.fromInteger(
                applicationData.getLcafApplicationDataAddr().getIpTos()).getHostAddress()));
        Ip locatorType = new IpBuilder().setIp(ip).setPort(applicationData.getLcafApplicationDataAddr().getLocalPort())
                .build();
        return locatorType;
    }

    public static long getLispInstanceId(LispAddressContainer container) {
        Address eid = container.getAddress();
        if (eid instanceof LcafSegment) {
            return ((LcafSegment) eid).getLcafSegmentAddr().getInstanceId();
        }
        return 0L;
    }

    public static String getAddressString(LispAddressContainer addr) {
        Address address = addr.getAddress();
        if (address instanceof LcafSourceDest) {
            return srcDstToString((LcafSourceDest) address);
        } else {
            LOG.warn("getAddressString called with unsupported address type:" + addr);
            return null;
        }
    }

    public static GetMappingInput buildGetMappingInput(LispAddressContainer eid, short mask) {
        return new GetMappingInputBuilder().setLispAddressContainer(eid).setMaskLength(mask).build();
    }

    public static AddMappingInput buildAddMappingInput(LispAddressContainer eid, List<LispAddressContainer> locators, int mask) {
        AddMappingInputBuilder mib = new AddMappingInputBuilder();

        mib.setAction(Action.NoAction).setAuthoritative(true).setLispAddressContainer(eid)
                .setLocatorRecord(buildLocatorRecords(locators)).setMapVersion((short) 0).setMaskLength((short) mask)
                .setRecordTtl(1440);
        return mib.build();
    }

    public static RemoveMappingInput buildRemoveMappingInput(LispAddressContainer eid) {
        RemoveMappingInputBuilder rmib = new RemoveMappingInputBuilder();
        rmib.setLispAddressContainer(eid);
        return rmib.build();
    }
}
