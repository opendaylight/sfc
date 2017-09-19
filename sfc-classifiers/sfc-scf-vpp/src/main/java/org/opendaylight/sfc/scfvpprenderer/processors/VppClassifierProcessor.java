/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.scfvpprenderer.processors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.util.vpp.SfcVppUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.ClassifyTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppClassifierProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VppClassifierProcessor.class);

    private final VppNodeManager nodeManager;
    private static final String SFC_BD_NAME = "SFCVPP";
    private static final String DUMMY_BD_NAME = "SFCDUMMY";
    private final Map<String, String> bridgeDomainCreated = new HashMap<>();

    private static class Pair<T> {
        private final T mask;
        private final T match;

        Pair(T mask, T match) {
            this.mask = mask;
            this.match = match;
        }

        T getMask() {
            return mask;
        }

        T getMatch() {
            return match;
        }
    }

    private static class SffInfo {
        private final DataBroker mountPoint;
        private final SffName sffName;
        private final IpAddress ip;
        private final Long pathId;
        private final Short serviceIndex;

        SffInfo(DataBroker mountPoint, SffName sffName, IpAddress ip, Long pathId, Short serviceIndex) {
            this.mountPoint = mountPoint;
            this.sffName = sffName;
            this.ip = ip;
            this.pathId = pathId;
            this.serviceIndex = serviceIndex;
        }
    }

    public VppClassifierProcessor(VppNodeManager nodeManager) {
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
    }

    private Optional<Acl> extractAcl(ServiceFunctionClassifier scf) {
        return Optional.ofNullable(scf)
                .map(ServiceFunctionClassifier::getAcl)
                .map(acl -> SfcProviderAclAPI.readAccessList(acl.getName(), acl.getType()));
    }

    private Optional<String> getInterfaceNameFromClassifier(SclServiceFunctionForwarder theClassifier) {
        return Optional.ofNullable(theClassifier)
                .filter(classifier -> classifier.getAttachmentPointType() instanceof Interface)
                .map(classifier -> (Interface) classifier.getAttachmentPointType())
                .map(Interface::getInterface);
    }

    private boolean validateInputs(Acl theAcl) {
        String aclName = theAcl.getAclName();
        if (aclName == null) {
            LOG.error("aclName is null");
            return false;
        }

        List<Ace> theAces = Optional.ofNullable(theAcl.getAccessListEntries())
                .map(AccessListEntries::getAce)
                .orElse(Collections.emptyList());

        if (theAces.isEmpty()) {
            LOG.error("acesList is null");
            return false;
        }

        return true;
    }

    private byte[] ipv4AddressPrefixToBytes(final Ipv4Prefix ipv4Prefix) {
        byte[] retval = new byte[4];
        String[] address = ipv4Prefix.getValue().substring(0, ipv4Prefix.getValue().indexOf('/')).split("\\.");
        String prefix = ipv4Prefix.getValue().substring(ipv4Prefix.getValue().indexOf('/') + 1);
        int mask = Short.parseShort(prefix);
        for (int i = mask % 8; i < 4; i++) {
            address[i] = "0";
        }

        for (int d = 0; d < 4; d++) {
            retval[d] = (byte) (Short.parseShort(address[d]) & 0xff);
        }
        return retval;
    }

    private byte[] ipv6AddressNoZoneToArray(final String address) {
        byte[] retval = new byte[16];

        //splits address and add ommited zeros for easier parsing
        List<String> segments = Arrays.asList(address.split(":"))
                .stream()
                .map(segment -> StringUtils.repeat('0', 4 - segment.length()) + segment)
                .collect(Collectors.toList());

        byte index = 0;
        for (String segment : segments) {

            String firstPart = segment.substring(0, 2);
            String secondPart = segment.substring(2);

            //first part should be ommited
            if ("00".equals(firstPart)) {
                index++;
            } else {
                retval[index++] = (byte) Short.parseShort(firstPart, 16);
            }

            retval[index++] = (byte) Short.parseShort(secondPart, 16);
        }

        return retval;
    }

    private byte[] ipv6AddressPrefixToBytes(final Ipv6Prefix ipv6Prefix) {
        return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(
                new Ipv6Address(ipv6Prefix.getValue().substring(0, ipv6Prefix.getValue().indexOf('/')))).getValue());
    }

    private Pair<HexString> getMaskAndMatch(Matches matches) {
        if (matches == null) {
            return null;
        }

        int maskLength = 0;
        String mask = "";
        String match = "";
        if (matches.getAceType() instanceof AceEth) {
            AceEth eth = (AceEth) matches.getAceType();

            if (eth.getDestinationMacAddress() != null) {
                mask = mask + "ff:ff:ff:ff:ff:ff";
                match = match + eth.getDestinationMacAddress().getValue();
            } else {
                mask = mask + "00:00:00:00:00:00";
                match = match + "00:00:00:00:00:00";
            }
            if (eth.getSourceMacAddress() != null) {
                mask = mask + ":ff:ff:ff:ff:ff:ff";
                match = match + ":" + eth.getSourceMacAddress().getValue();
            } else {
                mask = mask + ":00:00:00:00:00:00";
                match = match + ":00:00:00:00:00:00";
            }
            maskLength += 12;
        } else if (matches.getAceType() instanceof AceIp) {
            mask = mask + "00:00:00:00:00:00:00:00:00:00:00:00";
            match = match + "00:00:00:00:00:00:00:00:00:00:00:00";
            maskLength += 12;
            AceIp aceip = (AceIp) matches.getAceType();

            if (aceip.getAceIpVersion() instanceof AceIpv4) {
                //Ethernet Type
                mask = mask + ":ff:ff";
                match = match + ":08:00";
                maskLength += 2;

                //Transport Type
                mask = mask + ":00:00:00:00:00:00:00:00:00:ff:00:00";
                match = match + String.format(":00:00:00:00:00:00:00:00:00:%1$02x:00:00", aceip.getProtocol());
                maskLength += 12;

                //L3: IPv4
                AceIpv4 ipv4 = (AceIpv4) aceip.getAceIpVersion();
                Ipv4Prefix src = ipv4.getSourceIpv4Network();
                if (src != null) {
                    byte[] retval = ipv4AddressPrefixToBytes(src);
                    StringBuilder maskBuf = new StringBuilder(mask);
                    for (int i = 0; i < 4; i++) {
                        if (retval[i] == 0) {
                            maskBuf.append(":00");
                        } else {
                            maskBuf.append(":ff");
                        }
                    }

                    mask = maskBuf.toString();
                    match += String.format(":%1$02x:%2$02x:%3$02x:%4$02x", retval[0], retval[1], retval[2], retval[3]);
                } else {
                    mask = mask + ":00:00:00:00";
                    match = match + ":00:00:00:00";
                }
                maskLength += 4;

                Ipv4Prefix dst = ipv4.getDestinationIpv4Network();
                if (dst != null) {
                    byte[] retval = ipv4AddressPrefixToBytes(dst);
                    StringBuilder maskBuf = new StringBuilder(mask);
                    for (int i = 0; i < 4; i++) {
                        if (retval[i] == 0) {
                            maskBuf.append(":00");
                        } else {
                            maskBuf.append(":ff");
                        }
                    }

                    mask = maskBuf.toString();
                    match += String.format(":%1$02x:%2$02x:%3$02x:%4$02x", retval[0], retval[1], retval[2], retval[3]);
                } else {
                    mask = mask + ":00:00:00:00";
                    match = match + ":00:00:00:00";
                }
                maskLength += 4;
            } else if (aceip.getAceIpVersion() instanceof AceIpv6) {
                //Ethernet Type
                mask = mask + ":ff:ff";
                match = match + ":86:dd";
                maskLength += 2;

                //Transport Type
                mask = mask + ":00:00:00:00:00:00:ff:00";
                match = match + String.format(":00:00:00:00:00:00:%1$02x:00", aceip.getProtocol());
                maskLength += 8;

                //L3: IPv6
                AceIpv6 ipv6 = (AceIpv6) aceip.getAceIpVersion();
                Ipv6Prefix src = ipv6.getSourceIpv6Network();
                if (src != null) {
                    byte[] retval = ipv6AddressPrefixToBytes(src);
                    mask = mask + ":ff:ff:ff:ff:ff:ff:ff:ff::ff:ff:ff:ff:ff:ff:ff:ff";
                    match += String.format(":%1$02x:%2$02x:%3$02x:%4$02x:%5$02x:%6$02x:%7$02x:%8$02x:%9$02x:%10$02x:"
                            + "%11$02x:%12$02x:%13$02x:%14$02x:%15$02x:%16$02x", retval[0], retval[1], retval[2],
                            retval[3], retval[4], retval[5], retval[6], retval[7], retval[8], retval[9], retval[10],
                            retval[11], retval[12], retval[13], retval[14], retval[15]);
                } else {
                    mask = mask + ":00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";
                    match = match + ":00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";
                }
                maskLength += 16;

                Ipv6Prefix dst = ipv6.getDestinationIpv6Network();
                if (dst != null) {
                    byte[] retval = ipv6AddressPrefixToBytes(dst);
                    mask = mask + ":ff:ff:ff:ff:ff:ff:ff:ff::ff:ff:ff:ff:ff:ff:ff:ff";
                    match += String.format(":%1$02x:%2$02x:%3$02x:%4$02x:%5$02x:%6$02x:%7$02x:%8$02x:%9$02x:%10$02x"
                            + ":%11$02x:%12$02x:%13$02x:%14$02x:%15$02x:%16$02x", retval[0], retval[1], retval[2],
                            retval[3], retval[4], retval[5], retval[6], retval[7], retval[8], retval[9], retval[10],
                            retval[11], retval[12], retval[13], retval[14], retval[15]);
                } else {
                    mask = mask + ":00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";
                    match = match + ":00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";
                }
                maskLength += 16;
            }

            if (aceip.getProtocol() != null) {
                aceip.getProtocol();

                Integer srcPort = null;
                Integer dstPort = null;

                if (aceip.getSourcePortRange() != null && aceip.getSourcePortRange().getLowerPort() != null
                        && aceip.getSourcePortRange().getLowerPort().getValue() != null
                        && aceip.getSourcePortRange().getLowerPort().getValue() != 0) {
                    srcPort = aceip.getSourcePortRange().getLowerPort().getValue();
                }
                if (aceip.getDestinationPortRange() != null && aceip.getDestinationPortRange().getLowerPort() != null
                        && aceip.getDestinationPortRange().getLowerPort().getValue() != null
                        && aceip.getDestinationPortRange().getLowerPort().getValue() != 0) {
                    dstPort = aceip.getDestinationPortRange().getLowerPort().getValue();
                }

                // don't support port range
                if (srcPort != null) {
                    mask = mask + ":ff:ff";
                    match += String.format(":%1$02x:%2$02x", srcPort.intValue() & 0xFF00, srcPort.intValue() & 0x00FF);
                } else {
                    mask = mask + ":00:00";
                    match = match + ":00:00";
                }
                maskLength += 2;

                if (dstPort != null) {
                    mask = mask + ":ff:ff";
                    match += String.format(":%1$02x:%2$02x", dstPort.intValue() & 0xFF00, dstPort.intValue() & 0x00FF);
                } else {
                    mask = mask + ":00:00";
                    match = match + ":00:00";
                }
                maskLength += 2;
            }
        }
        if (maskLength == 0) {
            return null;
        } else {
            if (maskLength % 16 != 0) {
                int padLength = 16 - maskLength % 16;
                mask = mask + StringUtils.repeat(":00", padLength);
                match = match + StringUtils.repeat(":00", padLength);
            }
            return new Pair<>(new HexString(mask), new HexString(match));
        }
    }

    private RspName getReverseRspName(RspName rspName) {
        String name = rspName.getValue();
        if (name.endsWith("-Reverse")) {
            return new RspName(name.replaceAll("-Reverse", ""));
        } else {
            return new RspName(name + "-Reverse");
        }
    }

    private RenderedServicePath getRenderedServicePath(RspName rspName) {
        if (rspName == null) {
            LOG.error("rspName is null\n");
            return null;
        }

        return SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
    }

    private SffInfo getFirstSffInfoInRsp(RspName rspName) {
        RenderedServicePath renderedServicePath = getRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.error("renderedServicePath is null\n");
            return null;
        }

        Long pathId = renderedServicePath.getPathId();
        List<RenderedServicePathHop> hopList = renderedServicePath.getRenderedServicePathHop();
        if (hopList == null || hopList.isEmpty()) {
            LOG.error("Rendered path {} does not contain any hop", renderedServicePath.getName().getValue());
            return null;
        }

        RenderedServicePathHop firstRspHop = hopList.get(0);
        if (firstRspHop == null) {
            LOG.error("first rsp hop is null\n");
            return null;
        }

        Short serviceIndex = firstRspHop.getServiceIndex();
        SffName sffName = firstRspHop.getServiceFunctionForwarder();
        IpAddress sffIp = SfcVppUtils.getSffFirstDplIp(sffName);
        DataBroker mountPoint = SfcVppUtils.getSffMountpoint(this.nodeManager.getMountPointService(), sffName);
        return new SffInfo(mountPoint, sffName, sffIp, pathId, serviceIndex);
    }

    private boolean configureVxlanGpeClassifier(ServiceFunctionClassifier scf) {
        Optional<Acl> theAcl = extractAcl(scf);
        if (!theAcl.isPresent() || !validateInputs(theAcl.get())) {
            LOG.error("Could not retrieve the ACL from the classifier: {}", scf);
            return false;
        }
        Map<RspName, List<Pair<HexString>>> rspPairList = new HashMap<>();
        List<Ace> aceList = theAcl.get().getAccessListEntries().getAce();
        for (Ace ace : aceList) {
            Optional<RspName> rspName = Optional.ofNullable(ace.getActions())
                .map(theActions -> theActions.getAugmentation(Actions1.class))
                .map(actions1 -> (AclRenderedServicePath) actions1.getSfcAction())
                .map(aclRsp -> new RspName(aclRsp.getRenderedServicePath()));
            if (!rspName.isPresent()) {
                LOG.error("Could not retrieve the RSP from the classifier: {}", scf);
            }
            List<Pair<HexString>> pairList = rspPairList.get(rspName.get());
            if (pairList == null) {
                pairList = new ArrayList<>();
                rspPairList.put(rspName.get(), pairList);
            }
            pairList.add(getMaskAndMatch(ace.getMatches()));
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("sfflist is null");
            return false;
        }

        for (SclServiceFunctionForwarder sclSff : sfflist) {
            SffName sffName = new SffName(sclSff.getName());
            Optional<String> itfName = getInterfaceNameFromClassifier(sclSff);

            if (!itfName.isPresent()) {
                LOG.error("Could not get LogicalInterface from the classifier's attachment point");
            }

            IpAddress sffIp = SfcVppUtils.getSffFirstDplIp(sffName);
            DataBroker mountPoint = SfcVppUtils.getSffMountpoint(this.nodeManager.getMountPointService(), sffName);
            if (!bridgeDomainCreated.containsKey(sffName.getValue())) {
                SfcVppUtils.addDummyBridgeDomain(mountPoint, DUMMY_BD_NAME, sffName.getValue());
                SfcVppUtils.addDummyNshEntry(mountPoint, 0L, (short)1, sffName.getValue());
                SfcVppUtils.addDummyNshMap(mountPoint, 0L, (short)1, 0L, (short)1, "local0", sffName.getValue());
                SfcVppUtils.addBridgeDomain(mountPoint, SFC_BD_NAME, sffName.getValue());
                bridgeDomainCreated.put(sffName.getValue(), SFC_BD_NAME);
            }
            for (Entry<RspName, List<Pair<HexString>>> entry : rspPairList.entrySet()) {
                RspName rsp = entry.getKey();
                SffInfo sffInfo = getFirstSffInfoInRsp(rsp);
                RspName reverseRspName = getReverseRspName(rsp);
                RenderedServicePath reverseRenderedServicePath = getRenderedServicePath(reverseRspName);
                if (reverseRenderedServicePath == null) {
                    LOG.error("reverseRenderedServicePath is null\n");
                    return false;
                }

                final Long reversePathId = reverseRenderedServicePath.getPathId();
                List<RenderedServicePathHop> hopList = reverseRenderedServicePath.getRenderedServicePathHop();
                if (hopList == null || hopList.isEmpty()) {
                    LOG.error("Rendered path {} does not contain any hop",
                            reverseRenderedServicePath.getName().getValue());
                    return false;
                }

                RenderedServicePathHop lastRspHop = Iterables.getLast(hopList);
                if (lastRspHop == null) {
                    LOG.error("kast rsp hop is null\n");
                    return false;
                }

                final Short reverseServiceIndex = (short)(lastRspHop.getServiceIndex() - 1);
                List<Pair<HexString>> pairList = entry.getValue();
                int length = pairList.size();
                int index = 0;

                // Configure VPP classfier classify tables, sessions and enable ingress ACL
                List<ClassifyTableBuilder> classifyTableList = new ArrayList<>();
                List<ClassifySessionBuilder> classifySessionList = new ArrayList<>();
                for (Pair<HexString> maskMatch : pairList) {
                    boolean hasNext = false;
                    if (index < length - 1) {
                        hasNext = true;
                    }
                    ClassifyTableBuilder classifyTableBuilder = SfcVppUtils.buildVppClassifyTable(sffName,
                            rsp.getValue(), maskMatch.getMask(), hasNext);
                    classifyTableList.add(classifyTableBuilder);
                    classifySessionList.add(SfcVppUtils.buildVppClassifySession(classifyTableBuilder,
                            maskMatch.getMatch(), sffInfo.pathId, sffInfo.serviceIndex));
                    SfcVppUtils.increaseNextTableIndex(sffName.getValue());
                    index++;
                }
                SfcVppUtils.configureVppClassifier(mountPoint, sffName, classifyTableList, classifySessionList);

                //Enable Ingress Acl on table 0
                SfcVppUtils.enableIngressAcl(mountPoint, itfName.get(), SfcVppUtils.buildClassifyTableKey(0),
                        sffName.getValue());

                // Configure VPP classifier node
                SfcVppUtils.configureClassifierVxlanGpeNsh(mountPoint, sffName, SFC_BD_NAME, sffIp, sffInfo.ip,
                        sffInfo.pathId, sffInfo.serviceIndex);

                // For the traffic from the first SFF to VPP classifier node
                SfcVppUtils.addNshEntry(mountPoint, reversePathId, reverseServiceIndex, sffName.getValue());
                SfcVppUtils.addNshMapWithPop(mountPoint, reversePathId, reverseServiceIndex, reversePathId,
                        reverseServiceIndex, null, sffName.getValue());

                // Configure the first SFF, VPP renderer doesn't know this
                SfcVppUtils.configureVxlanGpeNsh(sffInfo.mountPoint, sffInfo.sffName, SFC_BD_NAME, sffInfo.ip, sffIp,
                        reversePathId, reverseServiceIndex);
            }
        }
        return true;
    }

    private boolean removeVxlanGpeClassifier(ServiceFunctionClassifier scf) {
        Optional<Acl> theAcl = extractAcl(scf);
        if (!theAcl.isPresent() || !validateInputs(theAcl.get())) {
            LOG.error("Could not retrieve the ACL from the classifier: {}", scf);
            return false;
        }
        Map<RspName, List<Pair<HexString>>> rspPairList = new HashMap<>();
        List<Ace> aceList = theAcl.get().getAccessListEntries().getAce();
        for (Ace ace : aceList) {
            Optional<RspName> rspName = Optional.ofNullable(ace.getActions())
                .map(theActions -> theActions.getAugmentation(Actions1.class))
                .map(actions1 -> (AclRenderedServicePath) actions1.getSfcAction())
                .map(aclRsp -> new RspName(aclRsp.getRenderedServicePath()));
            if (!rspName.isPresent()) {
                LOG.error("Could not retrieve the RSP from the classifier: {}", scf);
            }
            List<Pair<HexString>> pairList = rspPairList.get(rspName.get());
            if (pairList == null) {
                pairList = new ArrayList<>();
                rspPairList.put(rspName.get(), pairList);
            }
            pairList.add(getMaskAndMatch(ace.getMatches()));
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("sfflist is null");
            return false;
        }

        for (SclServiceFunctionForwarder sclSff : sfflist) {
            SffName sffName = new SffName(sclSff.getName());
            Optional<String> itfName = getInterfaceNameFromClassifier(sclSff);

            if (!itfName.isPresent()) {
                LOG.error("Could not get LogicalInterface from the classifier's attachment point");
            }

            IpAddress sffIp = SfcVppUtils.getSffFirstDplIp(sffName);
            DataBroker mountPoint = SfcVppUtils.getSffMountpoint(this.nodeManager.getMountPointService(), sffName);
            for (Entry<RspName, List<Pair<HexString>>> entry : rspPairList.entrySet()) {
                RspName rsp = entry.getKey();
                final SffInfo sffInfo = getFirstSffInfoInRsp(rsp);
                RspName reverseRspName = getReverseRspName(rsp);
                RenderedServicePath reverseRenderedServicePath = getRenderedServicePath(reverseRspName);
                if (reverseRenderedServicePath == null) {
                    LOG.error("reverseRenderedServicePath is null\n");
                    return false;
                }

                final Long reversePathId = reverseRenderedServicePath.getPathId();
                List<RenderedServicePathHop> hopList = reverseRenderedServicePath.getRenderedServicePathHop();
                if (hopList == null || hopList.isEmpty()) {
                    LOG.error("Rendered path {} does not contain any hop",
                            reverseRenderedServicePath.getName().getValue());
                    return false;
                }

                RenderedServicePathHop lastRspHop = Iterables.getLast(hopList);
                if (lastRspHop == null) {
                    LOG.error("kast rsp hop is null\n");
                    return false;
                }

                final Short reverseServiceIndex = (short)(lastRspHop.getServiceIndex() - 1);

                // Remove VPP classfier classify tables, sessions and disable ingress ACL
                List<Pair<HexString>> pairList = entry.getValue();
                int index = 0;
                List<String> tableKeyList = new ArrayList<>();
                List<HexString> matchList = new ArrayList<>();
                for (Pair<HexString> maskMatch : pairList) {
                    String classifyTableKey = SfcVppUtils.getSavedClassifyTableKey(sffName.getValue(), rsp.getValue(),
                            index);
                    tableKeyList.add(classifyTableKey);
                    matchList.add(maskMatch.getMatch());
                    index++;
                }
                // Disable Ingress Acl
                SfcVppUtils.disableIngressAcl(mountPoint, itfName.get(), SfcVppUtils.buildClassifyTableKey(0),
                        sffName.getValue());

                // Remove classify sessions and tables
                SfcVppUtils.removeVppClassifier(mountPoint, sffName, tableKeyList, matchList);

                // Remove NSH entry and map for the traffic from the first SFF to VPP classifier node
                SfcVppUtils.removeNshMap(mountPoint, reversePathId, reverseServiceIndex, reversePathId,
                        reverseServiceIndex, sffName.getValue());
                SfcVppUtils.removeNshEntry(mountPoint, reversePathId, reverseServiceIndex, sffName.getValue());

                // Remove configuration for the first SFF, VPP renderer doesn't know this
                SfcVppUtils.removeVxlanGpeNsh(sffInfo.mountPoint, sffInfo.sffName, sffInfo.ip, sffIp, reversePathId,
                        reverseServiceIndex);

                // Remove vxlan-gpe port and nsh entry and map for classifier
                SfcVppUtils.removeClassifierVxlanGpeNsh(mountPoint, sffName, SFC_BD_NAME, sffIp, sffInfo.ip,
                        sffInfo.pathId, sffInfo.serviceIndex);
            }
        }
        return true;
    }

    public void addScf(ServiceFunctionClassifier scf) {
        configureVxlanGpeClassifier(scf);
    }

    public void removeScf(ServiceFunctionClassifier scf) {
        removeVxlanGpeClassifier(scf);
    }

}
