/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_lisp.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.lispflowmapping.interfaces.lisp.IFlowMapping;
import org.opendaylight.lispflowmapping.type.AddressFamilyNumberEnum;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_lisp.provider.api.SfcLispFlowMappingApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.eidtolocatorrecords.EidToLocatorRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.LispAddressContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.LispAddressContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.DistinguishedName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.DistinguishedNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafApplicationData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.LcafSourceDest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.locatorrecords.LocatorRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mapping.database.rev150314.LfmMappingDatabaseService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LispUpdater implements ILispUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(LispUpdater.class);

    private static LispUpdater lispUpdaterObj;
//    private static IFlowMapping flowMapping;
    private LfmMappingDatabaseService lfmService;

    public LispUpdater(LfmMappingDatabaseService lfmService) {
        this.lfmService = lfmService;
        lispUpdaterObj = this;
    }

    public static LispUpdater getLispUpdaterObj() {
        return LispUpdater.lispUpdaterObj;
    }

    public LfmMappingDatabaseService getLfmMappingDatabaseService() {
        return lfmService;
    }

    public static void setFlowMapping(IFlowMapping fm) {
//        flowMapping = fm;
    }

    public LispAddressContainer asLispContainer(String rloc) {
        LispAddressContainerBuilder builder = new LispAddressContainerBuilder();
        builder.setAddress(buildDistinguishedNameAddress(rloc));
        return builder.build();
    }

    private DistinguishedName buildDistinguishedNameAddress(String rloc) {
        return new DistinguishedNameBuilder()
                .setDistinguishedName(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.control.plane.rev150314.lispaddress.lispaddresscontainer.address.distinguishedname.DistinguishedNameBuilder()
                                .setAfi(AddressFamilyNumberEnum.DISTINGUISHED_NAME.getIanaCode())
                                .setDistinguishedName(rloc).build()).build();
    }

    public ServiceFunctionForwarder updateLispData(ServiceFunctionForwarder serviceFunctionForwarder) {
        List<SffDataPlaneLocator> locations = serviceFunctionForwarder.getSffDataPlaneLocator();
        Lisp lispLocation = getLispLocationFromSff(locations);
        if (lispLocation != null) {
            return updateLispData(lispLocation, serviceFunctionForwarder);
        } else {
            return serviceFunctionForwarder;
        }
    }

    private Lisp getLispLocationFromSff(List<SffDataPlaneLocator> locations) {
        for (SffDataPlaneLocator location : locations) {
            DataPlaneLocator dpl = location.getDataPlaneLocator();
            LocatorType lt = dpl.getLocatorType();
            if (lt instanceof Lisp) {
                return (Lisp) lt;
            }
        }
        return null;
    }

    public boolean containsLispAddress(ServiceFunction serviceFunction) {
        List<SfDataPlaneLocator> locations = serviceFunction.getSfDataPlaneLocator();
        for (SfDataPlaneLocator location : locations) {
            if (location.getLocatorType() instanceof Lisp) {
                return true;
            }
        }
        return false;
    }

    public boolean containsLispAddress(ServiceFunctionForwarder serviceFunctionForwarder) {
        List<SffDataPlaneLocator> locations = serviceFunctionForwarder.getSffDataPlaneLocator();
        for (SffDataPlaneLocator location : locations) {
            if (location.getDataPlaneLocator().getLocatorType() instanceof Lisp) {
                return true;
            }
        }
        return false;
    }

    public ServiceFunction updateLispData(ServiceFunction serviceFunction) {
        List<SfDataPlaneLocator> locations = serviceFunction.getSfDataPlaneLocator();
        Lisp lispLocation = getLispLocationFromSf(locations);
        if (lispLocation != null) {
            return updateLispData(lispLocation, serviceFunction);
        } else {
            return serviceFunction;
        }
    }

    private Lisp getLispLocationFromSf(List<SfDataPlaneLocator> locations) {
        for (SfDataPlaneLocator location : locations) {
            LocatorType lt = location.getLocatorType();
            if (lt instanceof Lisp) {
                return (Lisp) lt;
            }
        }
        return null;
    }

    private ServiceFunction updateLispData(Lisp lispLocation, ServiceFunction serviceFunction) {
        Object[] methodParameters = { LispUtil.toContainer(lispLocation.getEid()) };
        @SuppressWarnings("unchecked")
        List<EidToLocatorRecord> reply = (List<EidToLocatorRecord>) SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.GET_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (reply == null || reply.isEmpty()) {
            return serviceFunction;
        }
        EidToLocatorRecord etlr = reply.get(0);
        for (LocatorRecord locator : etlr.getLocatorRecord()) {
            Address address = locator.getLispAddressContainer().getAddress();
            if (address instanceof LcafApplicationData) {
                LcafApplicationData applicationData = (LcafApplicationData) address;
                Ip locatorType = LispUtil.createLocator(applicationData);
                String name = lispLocation.getEid().toString();
                SfDataPlaneLocatorKey key = new SfDataPlaneLocatorKey(name);
                SfDataPlaneLocator loc = new SfDataPlaneLocatorBuilder().setLocatorType(locatorType).setKey(key)
                        .setName(name).build();
                ServiceFunctionBuilder fb = new ServiceFunctionBuilder(serviceFunction);
                fb.getSfDataPlaneLocator().add(loc);
                return fb.build();
            }

        }
        return serviceFunction;

    }

    private ServiceFunctionForwarder updateLispData(Lisp lispLocation, ServiceFunctionForwarder serviceFunctionForwarder) {
//        MapRequest mr = LispUtil.createMapRequest(lispLocation.getEid());
//        MapReply reply = flowMapping.handleMapRequest(mr);
//        if (reply.getEidToLocatorRecord() == null || reply.getEidToLocatorRecord().isEmpty()) {
//            return serviceFunctionForwarder;
//        }
        Object[] methodParameters = { LispUtil.toContainer(lispLocation.getEid()) };
        @SuppressWarnings("unchecked")
        List<EidToLocatorRecord> reply = (List<EidToLocatorRecord>) SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.GET_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (reply == null || reply.isEmpty()) {
            return serviceFunctionForwarder;
        }
        EidToLocatorRecord etlr = reply.get(0);
        for (LocatorRecord locator : etlr.getLocatorRecord()) {
            Address address = locator.getLispAddressContainer().getAddress();
            if (address instanceof LcafApplicationData) {
                LcafApplicationData applicationData = (LcafApplicationData) address;
                Ip locatorType = LispUtil.createLocator(applicationData);
                DataPlaneLocator dpl = new DataPlaneLocatorBuilder().setLocatorType(locatorType).build();
                String name = lispLocation.getEid().toString();
                SffDataPlaneLocatorKey key = new SffDataPlaneLocatorKey(name);
                SffDataPlaneLocator loc = new SffDataPlaneLocatorBuilder().setDataPlaneLocator(dpl).setKey(key)
                        .setName(name).build();
                ServiceFunctionForwarderBuilder fb = new ServiceFunctionForwarderBuilder(serviceFunctionForwarder);
                fb.getSffDataPlaneLocator().add(loc);
                return fb.build();
            }

        }
        return serviceFunctionForwarder;

    }

    public void registerPath(RenderedServicePath rsp) {
        // build locator paths from rsp hops and the locators of each src/dst pair of the associated acl's aces
        List<IpAddress> hopIpList = new ArrayList<IpAddress>();
        List<RenderedServicePathHop> hops = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop hop : hops) {
            String locatorName = hop.getServiceFunctionForwarderLocator();
            String sffName = hop.getServiceFunctionForwarder();
            LOG.debug("Looking up SFF {}", sffName);
            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff == null) {
                LOG.warn("Couldn't find SFF {} in datastore", sffName);
                return;
            }

            List<SffDataPlaneLocator> locators = sff.getSffDataPlaneLocator();
            for (SffDataPlaneLocator locator : locators) {
                if (locator.getName().equals(locatorName)) {
                    DataPlaneLocator dpLocator = locator.getDataPlaneLocator();
                    LOG.debug("Found for SFF {} the locator {}", sffName, dpLocator);
                    if (dpLocator.getLocatorType() instanceof Ip) {
                        Ip sffLocator = (Ip) dpLocator.getLocatorType();
                        hopIpList.add(sffLocator.getIp());
                    }
                }
            }
        }

        // get rsp's acl
        AccessList acl = SfcLispUtil.getServiceFunctionAcl(rsp.getParentServiceFunctionPath());
        List<AccessListEntries> acesList = acl.getAccessListEntries();

        // for each of acl's aces get src/dst ips ...
        for(AccessListEntries aces: acesList) {
            Matches matches = aces.getMatches();
            if (matches.getAceType() instanceof AceIp) {
                AceIp ipMatch = (AceIp) matches.getAceType();
                LcafSourceDest srcDst = getSrcDstFromAce(ipMatch);

                if (srcDst != null) {
                    // ... find locator of dst eid ...
                    IpAddress lastHop = findLastHop(LispUtil.toContainer(srcDst.getLcafSourceDestAddr().getDstAddress()
                            .getPrimitiveAddress()));
                    if (lastHop !=null) {
                        hopIpList.add(lastHop);
                        // ... build a TE LCAF with the just found locator as last hop and register it with lfm.
                        // NOTE: We contemplate only the case when dst has an associated mapping in lfm's db, as the
                        // insertion of a new src/dst mapping does not affect it. If however, a src/dst mapping does
                        // exist, we overwrite it lower, thus this might require fixing.  XXX
                        buildAndRegisterMapping(srcDst, hopIpList);
                    } else {
                        LOG.debug("Couldn't find locator for src/dst eid: {}", srcDst);
                    }
                } else {
                    LOG.debug("Couldn't parse src/dst prefixes for ACE: {}", ipMatch);
                }

            }
        }
    }


    @SuppressWarnings("unchecked")
    private IpAddress findLastHop(LispAddressContainer eid) {
        Object[] methodParameters = { eid };
        List<EidToLocatorRecord> reply = (List<EidToLocatorRecord>) SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.GET_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (reply == null || reply.isEmpty()) {
            return null;
        }

        EidToLocatorRecord etlr = reply.get(0);
        for (LocatorRecord locator : etlr.getLocatorRecord()) {
            Address address = locator.getLispAddressContainer().getAddress();
            if (address instanceof Ipv4) {
                Ipv4 ipv4 = (Ipv4) address;
                return new IpAddress(ipv4.getIpv4Address().getIpv4Address());
            } else if (address instanceof Ipv6) {
                Ipv6 ipv6 = (Ipv6) address;
                return new IpAddress(ipv6.getIpv6Address().getIpv6Address());
            } else {
                LOG.debug("Locator address type not supported for TE LCAF: {}", address);
            }
        }
        return null;
    }

    private void buildAndRegisterMapping(LcafSourceDest eid, List<IpAddress> hopList) {
        LispAddressContainer locatorPath = LispUtil.asLispContainer(LispUtil.buildTeLcaf(hopList));
        List<LispAddressContainer> locators = Arrays.asList(locatorPath);
        Object[] methodParameters = { LispUtil.asLispContainer(eid), locators };
        SfcLispUtil.submitCallable(new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.ADD_MAPPING,
                methodParameters), OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
    }

    public void deletePath(RenderedServicePath rsp) {
        // get rsp's acl
        AccessList acl = SfcLispUtil.getServiceFunctionAcl(rsp.getParentServiceFunctionPath());
        List<AccessListEntries> acesList = acl.getAccessListEntries();

        // for each of acl's aces get src/dst ips ...
        for(AccessListEntries aces: acesList) {
            Matches matches = aces.getMatches();
            if (matches.getAceType() instanceof AceIp) {
                AceIp ipMatch = (AceIp) matches.getAceType();
                LcafSourceDest srcDst = getSrcDstFromAce(ipMatch);
                removeMapping(srcDst);
            }
        }
    }

    private LcafSourceDest getSrcDstFromAce(AceIp ipMatch) {
        AceIpVersion ipMatchVersion = ipMatch.getAceIpVersion();
        String[] srcPrefixParts = null, dstPrefixParts = null;

        if (ipMatchVersion instanceof AceIpv4) {
            AceIpv4 ipMatch4 = (AceIpv4) ipMatchVersion;
            srcPrefixParts = ipMatch4.getSourceIpv4Address().getValue().split("/");
            dstPrefixParts = ipMatch4.getDestinationIpv4Address().getValue().split("/");
        } else if (ipMatchVersion instanceof AceIpv6) {
            AceIpv6 ipMatch6 = (AceIpv6) ipMatchVersion;
            srcPrefixParts = ipMatch6.getSourceIpv6Address().getValue().split("/");
            dstPrefixParts = ipMatch6.getDestinationIpv6Address().getValue().split("/");
        }

        if (srcPrefixParts != null && srcPrefixParts.length == 2 && dstPrefixParts != null
                && dstPrefixParts.length == 2) {
            return LispUtil.buildSrcDst(srcPrefixParts[0], dstPrefixParts[0],
                    Integer.parseInt(srcPrefixParts[1]), Integer.parseInt(dstPrefixParts[1]));
        } else {
            LOG.debug("Couldn't parse src/dst prefixes for ACE: {}", ipMatch);
            return null;
        }
    }

    private void removeMapping(LcafSourceDest eid) {
        Object[] methodParameters = { LispUtil.asLispContainer(eid)};
        SfcLispUtil.submitCallable(new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.DELETE_MAPPING,
                methodParameters), OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
    }

    public void updatePath(RenderedServicePath newRsp, RenderedServicePath oldRsp) {
        AccessList newAcl = SfcLispUtil.getServiceFunctionAcl(newRsp.getParentServiceFunctionPath());
        AccessList oldAcl = SfcLispUtil.getServiceFunctionAcl(oldRsp.getParentServiceFunctionPath());
        if (shallowCompareAcls(newAcl, oldAcl)) {
            // overwrite
            registerPath(newRsp);
        } else {
            deletePath(oldRsp);
            registerPath(newRsp);
        }
    }

    private boolean shallowCompareAcls(AccessList acl1, AccessList acl2) {
        if (!acl1.getAclName().equals(acl2.getAclName())) {
            return false;
        }
        List<AccessListEntries> aces1 = acl1.getAccessListEntries();
        List<AccessListEntries> aces2 = acl2.getAccessListEntries();
        if (aces1.size() != aces2.size()) {
            return false;
        }

        for (AccessListEntries it1 : aces1) {
            Matches matches1 = it1.getMatches();
            if (!(matches1.getAceType() instanceof AceIp)) {
                continue;
            }
            AceIp ipMatch1 = (AceIp) matches1.getAceType();
            LcafSourceDest sd1 = getSrcDstFromAce(ipMatch1);

            boolean found = false;
            for (AccessListEntries it2: aces2) {
                if (it1.getRuleName().equals(it2.getRuleName())) {
                    found = true;
                    Matches matches2 = it2.getMatches();
                    if (!(matches2.getAceType() instanceof AceIp)) {
                        continue;
                    }
                    AceIp ipMatch2 = (AceIp) matches2.getAceType();
                    LcafSourceDest sd2 = getSrcDstFromAce(ipMatch2);
                    if (!sd1.equals(sd2)) {
                        return false;
                    }
                }
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }
}
