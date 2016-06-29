/*
 * Copyright (c) 2014 Contextream, Inc. and others. All rights reserved.
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
import org.opendaylight.lispflowmapping.lisp.util.LispAddressUtil;
import org.opendaylight.lispflowmapping.lisp.util.SourceDestKeyHelper;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_lisp.provider.api.SfcLispFlowMappingApi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.ApplicationData;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.eid.container.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.locatorrecords.LocatorRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.mapping.record.container.MappingRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.rloc.container.Rloc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.OdlMappingserviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LispUpdater implements ILispUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(LispUpdater.class);

    private static LispUpdater lispUpdaterObj;
    private OdlMappingserviceService lfmService;

    public LispUpdater(OdlMappingserviceService lfmService) {
        this.lfmService = lfmService;
        lispUpdaterObj = this;
    }

    public static LispUpdater getLispUpdaterObj() {
        return LispUpdater.lispUpdaterObj;
    }

    public OdlMappingserviceService getMappingserviceService() {
        return lfmService;
    }

    public static void setFlowMapping(IFlowMapping fm) {
        // flowMapping = fm;
    }
/*
    private Rloc buildDistinguishedNameAddress(String rloc) {
        RlocBuilder dnb = new RlocBuilder();
        dnb.setAddressType(DistinguishedNameAfi.class);
        dnb.setAddress(new DistinguishedNameBuilder().setDistinguishedName(new DistinguishedNameType(rloc)).build());
        return dnb.build();
    }
*/
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
        Object[] methodParameters = { LispAddressUtil.toIpPrefixEid(lispLocation.getEid(), 0) };
        MappingRecord reply = (MappingRecord) SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.GET_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (reply == null) {
            return serviceFunction;
        }
        for (LocatorRecord locator : reply.getLocatorRecord()) {
            Address address = locator.getRloc().getAddress();
            if (address instanceof ApplicationData) {
                ApplicationData applicationData = (ApplicationData) address;
                Ip locatorType = SfcLispUtil.createLocator(applicationData);
                SfDataPlaneLocatorName name = new SfDataPlaneLocatorName(lispLocation.getEid().toString());
                SfDataPlaneLocatorKey key = new SfDataPlaneLocatorKey(name);
                SfDataPlaneLocator loc =
                        new SfDataPlaneLocatorBuilder().setLocatorType(locatorType).setKey(key).setName(name).build();
                ServiceFunctionBuilder fb = new ServiceFunctionBuilder(serviceFunction);
                fb.getSfDataPlaneLocator().add(loc);
                return fb.build();
            }

        }
        return serviceFunction;
    }

    private ServiceFunctionForwarder updateLispData(Lisp lispLocation,
            ServiceFunctionForwarder serviceFunctionForwarder) {
        // MapRequest mr = LispUtil.createMapRequest(lispLocation.getEid());
        // MapReply reply = flowMapping.handleMapRequest(mr);
        // if (reply.getEidToLocatorRecord() == null || reply.getEidToLocatorRecord().isEmpty()) {
        // return serviceFunctionForwarder;
        // }
        Object[] methodParameters = { LispAddressUtil.toIpPrefixEid(lispLocation.getEid(), 0) };
        MappingRecord reply = (MappingRecord) SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.GET_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (reply == null ) {
            return serviceFunctionForwarder;
        }

        for (LocatorRecord locator : reply.getLocatorRecord()) {
            Address address = locator.getRloc().getAddress();
            if (address instanceof ApplicationData) {
                ApplicationData applicationData = (ApplicationData) address;
                Ip locatorType = SfcLispUtil.createLocator(applicationData);
                DataPlaneLocator dpl = new DataPlaneLocatorBuilder().setLocatorType(locatorType).build();
                SffDataPlaneLocatorName name = new SffDataPlaneLocatorName(lispLocation.getEid().toString());
                SffDataPlaneLocatorKey key = new SffDataPlaneLocatorKey(name);
                SffDataPlaneLocator loc =
                        new SffDataPlaneLocatorBuilder().setDataPlaneLocator(dpl).setKey(key).setName(name).build();
                ServiceFunctionForwarderBuilder fb = new ServiceFunctionForwarderBuilder(serviceFunctionForwarder);
                fb.getSffDataPlaneLocator().add(loc);
                return fb.build();
            }

        }
        return serviceFunctionForwarder;
    }

    private boolean isIpInList(List<IpAddress> ipList, IpAddress newIp) {
        for (IpAddress ip : ipList) {
            if (newIp.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    private boolean addIfNotInList(List<IpAddress> ipList, IpAddress newIp) {
        if (newIp == null) {
            return false;
        }
        if (!isIpInList(ipList, newIp)) {
            ipList.add(newIp);
            return true;
        }
        return false;
    }

    private IpAddress findLastHop(Eid prefix) {
        Object[] methodParameters = { prefix };
        MappingRecord reply = (MappingRecord) SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.GET_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
        if (reply == null) {
            return null;
        }

        for (LocatorRecord locator : reply.getLocatorRecord()) {
            Address address = locator.getRloc().getAddress();
            if (address instanceof Ipv4) {
                Ipv4 ipv4 = (Ipv4) address;
                return new IpAddress(ipv4.getIpv4());
            } else if (address instanceof Ipv6) {
                Ipv6 ipv6 = (Ipv6) address;
                return new IpAddress(ipv6.getIpv6());
            } else {
                LOG.debug("Locator address type not supported for TE LCAF: {}", address);
            }
        }
        return null;
    }

    private void buildAndRegisterTeMapping(Eid eid, List<IpAddress> hopList) {
        Rloc locatorPath = LispAddressUtil.asTeLcafRloc(hopList);
        List<Rloc> locators = Arrays.asList(locatorPath);
        Object[] methodParameters = {eid, locators};
        SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.ADD_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
    }

    private Eid getSrcDstFromAce(AceIp ipMatch) {
        AceIpVersion ipMatchVersion = ipMatch.getAceIpVersion();
        String[] srcPrefixParts = null, dstPrefixParts = null;

        if (ipMatchVersion instanceof AceIpv4) {
            AceIpv4 ipMatch4 = (AceIpv4) ipMatchVersion;
            srcPrefixParts = ipMatch4.getSourceIpv4Network().getValue().split("/");
            dstPrefixParts = ipMatch4.getDestinationIpv4Network().getValue().split("/");
        } else if (ipMatchVersion instanceof AceIpv6) {
            AceIpv6 ipMatch6 = (AceIpv6) ipMatchVersion;
            srcPrefixParts = ipMatch6.getSourceIpv6Network().getValue().split("/");
            dstPrefixParts = ipMatch6.getDestinationIpv6Network().getValue().split("/");
        }

        if (srcPrefixParts != null && srcPrefixParts.length == 2 && dstPrefixParts != null
                && dstPrefixParts.length == 2) {
            return LispAddressUtil.asSrcDstEid(srcPrefixParts[0], dstPrefixParts[0], Integer.parseInt(srcPrefixParts[1]),
                    Integer.parseInt(dstPrefixParts[1]), 0);
        } else {
            LOG.debug("Couldn't parse src/dst prefixes for ACE: {}", ipMatch);
            return null;
        }
    }

    @Deprecated
    public void registerPathOld(RenderedServicePath rsp) {
        // build locator paths from rsp hops and the locators of each src/dst pair of the associated
        // acl's aces
        List<IpAddress> hopIpList = new ArrayList<IpAddress>();
        List<RenderedServicePathHop> hops = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop hop : hops) {
            SffDataPlaneLocatorName locatorName = hop.getServiceFunctionForwarderLocator();
            SffName sffName = hop.getServiceFunctionForwarder();
            LOG.debug("Looking up SFF {}", sffName);
            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff == null) {
                LOG.warn("Couldn't find SFF {} in datastore", sffName);
                return;
            }

            List<SffDataPlaneLocator> locators = sff.getSffDataPlaneLocator();

            boolean found = false;
            for (SffDataPlaneLocator locator : locators) {
                if (locator.getName().equals(locatorName)) {
                    DataPlaneLocator dpLocator = locator.getDataPlaneLocator();
                    LOG.debug("Found for SFF {} the locator {}", sffName, dpLocator);
                    if (dpLocator.getLocatorType() instanceof Ip) {
                        Ip sffLocator = (Ip) dpLocator.getLocatorType();
                        // For now we do not support an SFF appearing twice on a TE path
                        // and we only use one SFF locator
                        if (sffLocator != null) {
                            addIfNotInList(hopIpList, sffLocator.getIp());
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                LOG.debug("Couldn't find locator for SFF {}. Aborting!", sff);
                return;
            }
        }

        // get rsp's acl
        Acl acl = SfcLispUtil.getServiceFunctionAcl(rsp.getParentServiceFunctionPath());
        if (acl == null) {
            LOG.debug("ACL for RSP is null, can't register TE path with LISP!");
            return;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.debug("AccessListEntries for RSP is null, can't register TE path with LISP!");
            return;
        }

        List<Ace> acesList = accessListEntries.getAce();

        // for each of acl's aces get src/dst ips ...
        for (Ace aces : acesList) {
            Matches matches = aces.getMatches();
            if (matches.getAceType() instanceof AceIp) {
                AceIp ipMatch = (AceIp) matches.getAceType();
                Eid srcDst = getSrcDstFromAce(ipMatch);

                if (srcDst == null) {
                    LOG.debug("Couldn't parse src/dst prefixes for ACE: {}", ipMatch);
                    return;
                }

                // ... find locator of dst eid ...
                Eid dstPrefix = SourceDestKeyHelper.getDst(srcDst);
                IpAddress lastHop = findLastHop(dstPrefix);
                if (lastHop == null) {
                    LOG.debug("Couldn't find locator for src/dst eid: {}", srcDst);
                    return;
                }

                LOG.debug("Found last hop {}", lastHop);
                if (isIpInList(hopIpList, lastHop)) {
                    if (hopIpList.get(hopIpList.size() - 1).equals(lastHop)) {
                        LOG.debug("Last hop is already on the last position in the list of hops!");
                    } else {
                        LOG.debug("Last hop is already in the list of hops, but not last. Not supported!");
                        return;
                    }
                } else {
                    hopIpList.add(lastHop);
                }
                // ... build a TE LCAF with the just found locator as last hop and register it with lfm.
                // NOTE: We contemplate only the case when dst has an associated mapping in lfm's db, as the
                // insertion of a new src/dst mapping does not affect it. If however, a src/dst mapping does
                // exist, we overwrite it lower, thus this might require fixing. XXX
                buildAndRegisterTeMapping(srcDst, hopIpList);
            }
        }
    }

    private void registerMapping(Eid eid, Rloc rloc) {
        List<Rloc> locators = Arrays.asList(rloc);
        Object[] methodParameters = {eid, locators};
        SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.ADD_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
    }

    private void registerElpMapping(Eid eid, List<IpAddress> hopList) {
        Rloc locatorPath = LispAddressUtil.asTeLcafRloc(hopList);
        registerMapping(eid, locatorPath);
    }

    public void registerPath(RenderedServicePath rsp) {
        // build locator paths from rsp hops and the locators of each src/dst pair of the associated
        // acl's aces
        List<IpAddress> hopIpList = new ArrayList<IpAddress>();
        List<RenderedServicePathHop> hops = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop hop : hops) {
            SffDataPlaneLocatorName locatorName = hop.getServiceFunctionForwarderLocator();
            SffName sffName = hop.getServiceFunctionForwarder();
            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff == null) {
                LOG.warn("Couldn't find SFF {} that supports hop {} in datastore", sffName,
                        hop.getHopNumber().shortValue());
                return;
            }

            List<SffDataPlaneLocator> locators = sff.getSffDataPlaneLocator();

            boolean found = false;
            for (SffDataPlaneLocator locator : locators) {
                if (locator.getName().equals(locatorName)) {
                    DataPlaneLocator dpLocator = locator.getDataPlaneLocator();
                    LOG.debug("Found for SFF {} the locator {}", sffName, dpLocator);
                    if (dpLocator.getLocatorType() instanceof Ip) {
                        Ip sffLocator = (Ip) dpLocator.getLocatorType();
                        // For now we do not support an SFF appearing twice on a TE path
                        // and we only use one SFF locator
                        if (sffLocator != null) {
                            hopIpList.add(sffLocator.getIp());
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                LOG.debug("Couldn't find locator for SFF {}. Aborting!", sff);
                return;
            }
        }

        // TODO fix VNI. The RSP's tenant ID is a string (UUID?) we need a long
        Eid spEid = LispAddressUtil.asServicePathEid((long) 0, rsp.getPathId().longValue(),
                LispAddressUtil.STARTING_SERVICE_INDEX);
        registerElpMapping(spEid, hopIpList);

        // TODO for each ACE in the RSP's ACL we should insert a SourceDest mapping pointing at the ServicePath LCAF.
        // For now we can't because we default to SourceDest mapping lookups in LFM so both the classifier and the end
        // of chain xTR would retrieve the same mapping. The solution is to use an ELP as a locator BUT at this time
        // the hops can only be SimpleAddresses. The ServicePath LCAF is not one.
    }

    private void removeMapping(Eid eid) {
        Object[] methodParameters = {eid};
        SfcLispUtil.submitCallable(
                new SfcLispFlowMappingApi(lfmService, SfcLispFlowMappingApi.Method.DELETE_MAPPING, methodParameters),
                OpendaylightSfc.getOpendaylightSfcObj().getExecutor());
    }

    @Deprecated
    public void deletePathOld(RenderedServicePath rsp) {
        // get rsp's acl
        Acl acl = SfcLispUtil.getServiceFunctionAcl(rsp.getParentServiceFunctionPath());
        if (acl != null) {
            List<Ace> acesList = acl.getAccessListEntries().getAce();

            // for each of acl's aces get src/dst ips ...
            for (Ace aces : acesList) {
                Matches matches = aces.getMatches();
                if (matches.getAceType() instanceof AceIp) {
                    AceIp ipMatch = (AceIp) matches.getAceType();
                    Eid srcDst = getSrcDstFromAce(ipMatch);
                    removeMapping(srcDst);
                }
            }
        }
    }

    public void deletePath(RenderedServicePath rsp) {
        // remove ServicePath mapping
        Eid spEid = LispAddressUtil.asServicePathEid((long) 0, rsp.getPathId().longValue(),
                LispAddressUtil.STARTING_SERVICE_INDEX);
        removeMapping(spEid);
    }

    private boolean shallowCompareAcls(Acl acl1, Acl acl2) {
        if (acl1 != null && acl2 == null) {
            return false;
        } else if (acl1 == null && acl2 != null) {
            return false;
        } else if (acl1 == null && acl2 == null) {
            return true;
        }

        if (!acl1.getAclName().equals(acl2.getAclName())) {
            return false;
        }
        List<Ace> aces1 = acl1.getAccessListEntries().getAce();
        List<Ace> aces2 = acl2.getAccessListEntries().getAce();
        if (aces1.size() != aces2.size()) {
            return false;
        }

        for (Ace it1 : aces1) {
            Matches matches1 = it1.getMatches();
            if (!(matches1.getAceType() instanceof AceIp)) {
                continue;
            }
            AceIp ipMatch1 = (AceIp) matches1.getAceType();
            Eid sd1 = getSrcDstFromAce(ipMatch1);

            boolean found = false;
            for (Ace it2 : aces2) {
                if (it1.getRuleName().equals(it2.getRuleName())) {
                    found = true;
                    Matches matches2 = it2.getMatches();
                    if (!(matches2.getAceType() instanceof AceIp)) {
                        continue;
                    }
                    AceIp ipMatch2 = (AceIp) matches2.getAceType();
                    Eid sd2 = getSrcDstFromAce(ipMatch2);
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

    @Deprecated
    public void updatePathOld(RenderedServicePath newRsp, RenderedServicePath oldRsp) {
        Acl newAcl = SfcLispUtil.getServiceFunctionAcl(newRsp.getParentServiceFunctionPath());
        Acl oldAcl = SfcLispUtil.getServiceFunctionAcl(oldRsp.getParentServiceFunctionPath());
        if (shallowCompareAcls(newAcl, oldAcl)) {
            // overwrite
            registerPath(newRsp);
        } else {
            deletePath(oldRsp);
            registerPath(newRsp);
        }
    }

    public void updatePath(RenderedServicePath newRsp, RenderedServicePath oldRsp) {
        // overwrite
        registerPath(newRsp);
    }
}
