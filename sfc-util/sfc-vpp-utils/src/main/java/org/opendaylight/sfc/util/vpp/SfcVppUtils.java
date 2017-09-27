/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.util.vpp;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.SfLocatorProxyAugmentation;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.proxy.ProxyDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VxlanGpeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classfier.acl.rev161214.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classfier.acl.rev161214.acl.base.attributes.Ip4AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.OpaqueIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.None;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.NshMdType1Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.NshMdType1AugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.Pop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.Push;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.Swap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.nsh.entries.NshEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.nsh.maps.NshMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev161214.vpp.nsh.nsh.maps.NshMapKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcVppUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SfcVppUtils.class);
    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_IID = InstanceIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).build();
    private static final ConcurrentMap<String, Integer> TABLE_INDICE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> FIRST_TABLE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<String>> RSP_TABLE_ID_LIST = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ConcurrentMap<String, AtomicInteger>> VXLAN_GPER_PORT_REF_CNT =
            new ConcurrentHashMap<>();

    private SfcVppUtils() {
    }

    public static DataBroker getSffMountpoint(MountPointService mountService, SffName sffName) {
        final NodeId nodeId = new NodeId(sffName.getValue());
        InstanceIdentifier<Node> netconfNodeIid = NETCONF_TOPOLOGY_IID.child(Node.class,
                new NodeKey(new NodeId(nodeId)));
        Optional<MountPoint> optionalObject = mountService.getMountPoint(netconfNodeIid);
        if (optionalObject.isPresent()) {
            MountPoint mountPoint = optionalObject.get();
            if (mountPoint != null) {
                Optional<DataBroker> optionalDataBroker = mountPoint.getService(DataBroker.class);
                if (optionalDataBroker.isPresent()) {
                    return optionalDataBroker.get();
                }
            }
        }
        return null;
    }

    private static ServiceFunctionDictionary getSfDictionary(SffName sffName, SfName sfName) {
        ServiceFunctionDictionary sfDictionary = null;
        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        if (sff == null) {
            return null;
        }

        List<ServiceFunctionDictionary> sfdList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary sfd : sfdList) {
            if (sfd.getName().equals(sfName)) {
                sfDictionary = sfd;
                break;
            }
        }

        return sfDictionary;
    }

    private static IpAddress getSffDplIp(SffName sffName, SffDataPlaneLocatorName sffDplName) {
        IpAddress ip = null;
        SffDataPlaneLocator sffDpl = SfcProviderServiceForwarderAPI
                .readServiceFunctionForwarderDataPlaneLocator(sffName, sffDplName);
        if (sffDpl == null) {
            return null;
        }

        DataPlaneLocator dpl = sffDpl.getDataPlaneLocator();
        if (dpl == null) {
            return null;
        }

        LocatorType locatorType = dpl.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }

        return ip;
    }

    public static IpAddress getSffFirstDplIp(SffName sffName) {
        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        List<SffDataPlaneLocator> sffDataPlaneLocatorList = sff.getSffDataPlaneLocator();
        if (sffDataPlaneLocatorList == null || sffDataPlaneLocatorList.isEmpty()) {
            return null;
        }
        SffDataPlaneLocator sffDpl = sffDataPlaneLocatorList.get(0);
        if (sffDpl == null) {
            return null;
        }

        DataPlaneLocator dpl = sffDpl.getDataPlaneLocator();
        if (dpl == null) {
            return null;
        }

        IpAddress ip = null;
        LocatorType locatorType = dpl.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }
        return ip;
    }

    public static IpAddress getSfProxyDplIp(SfLocatorProxyAugmentation augment) {
        IpAddress ip = null;
        ProxyDataPlaneLocator proxyDpl = augment.getProxyDataPlaneLocator();

        if (proxyDpl == null) {
            return null;
        }

        LocatorType locatorType = proxyDpl.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }

        return ip;
    }

    private static IpAddress getSfDplIp(SfName sfName, SfDataPlaneLocatorName sfDplName) {
        IpAddress ip = null;
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        if (serviceFunction == null) {
            return null;
        }

        List<SfDataPlaneLocator> sdDplList = serviceFunction.getSfDataPlaneLocator();
        SfDataPlaneLocator sfDataPlaneLocator = null;
        for (SfDataPlaneLocator sfDpl : sdDplList) {
            if (sfDpl.getName().equals(sfDplName)) {
                sfDataPlaneLocator = sfDpl;
                break;
            }
        }

        if (sfDataPlaneLocator == null) {
            return null;
        }

        SfLocatorProxyAugmentation sfDplProxyAug = sfDataPlaneLocator.getAugmentation(SfLocatorProxyAugmentation.class);
        if (sfDplProxyAug != null) {
            return SfcVppUtils.getSfProxyDplIp(sfDplProxyAug);
        }

        LocatorType locatorType = sfDataPlaneLocator.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }

        return ip;
    }

    public static List<IpAddress> getSffSfIps(final SffName sffName, final SfName sfName) {
        ServiceFunctionDictionary sfDictionary;
        SfDataPlaneLocatorName sfDplName;

        sfDictionary = getSfDictionary(sffName, sfName);
        if (sfDictionary == null) {
            return null;
        }

        sfDplName = sfDictionary.getSffSfDataPlaneLocator().getSfDplName();
        SffDataPlaneLocatorName sffDplName;
        sffDplName = sfDictionary.getSffSfDataPlaneLocator().getSffDplName();

        IpAddress localIp;
        localIp = getSffDplIp(sffName, sffDplName);
        if (localIp == null) {
            return null;
        }
        List<IpAddress> ipList = new ArrayList<>();
        ipList.add(localIp);

        IpAddress remoteIp;
        remoteIp = getSfDplIp(sfName, sfDplName);
        if (remoteIp == null) {
            return null;
        }
        ipList.add(remoteIp);
        return ipList;
    }

    private static void addFuturesCallback(final WriteTransaction transaction) {
        Futures.addCallback(transaction.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable throwable) {
            }
        }, MoreExecutors.directExecutor());
    }

    public static void addDummyBridgeDomain(final DataBroker dataBroker, String bridgeDomainName, String vppNode) {
        BridgeDomainBuilder bdBuilder = new BridgeDomainBuilder();
        bdBuilder.setName(bridgeDomainName);
        bdBuilder.setFlood(true);
        bdBuilder.setForward(true);
        bdBuilder.setLearn(true);
        bdBuilder.setUnknownUnicastFlood(true);
        bdBuilder.setArpTermination(false);

        List<BridgeDomain> bdList = new ArrayList<>();
        bdList.add(bdBuilder.build());
        BridgeDomainsBuilder bdsBuilder = new BridgeDomainsBuilder();
        bdsBuilder.setBridgeDomain(bdList);

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<BridgeDomains> bridgeDomainsIId = InstanceIdentifier.create(Vpp.class)
                .child(BridgeDomains.class);
        wTx.put(LogicalDatastoreType.CONFIGURATION, bridgeDomainsIId, bdsBuilder.build());
        addFuturesCallback(wTx);
    }

    public static void addBridgeDomain(final DataBroker dataBroker, String bridgeDomainName, String vppNode) {
        BridgeDomainBuilder bdBuilder = new BridgeDomainBuilder();
        bdBuilder.setName(bridgeDomainName);
        bdBuilder.setFlood(true);
        bdBuilder.setForward(true);
        bdBuilder.setLearn(true);
        bdBuilder.setUnknownUnicastFlood(true);
        bdBuilder.setArpTermination(false);

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<BridgeDomain> bridgeDomainIId = InstanceIdentifier.create(Vpp.class)
                .child(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(bridgeDomainName));
        wTx.put(LogicalDatastoreType.CONFIGURATION, bridgeDomainIId, bdBuilder.build());
        addFuturesCallback(wTx);
    }

    public static String buildVxlanGpePortKey(final IpAddress remote) {
        return "vxlanGpeTun" + "_" + remote.getIpv4Address().getValue();
    }

    private static int incrementVxlanGpeRefCnt(final String vxlanGpePortKey, final String vppNode) {
        final ConcurrentMap<String, AtomicInteger> nodeMap = VXLAN_GPER_PORT_REF_CNT.computeIfAbsent(
                vppNode, key -> new ConcurrentHashMap<>());

        final AtomicInteger count = nodeMap.computeIfAbsent(vxlanGpePortKey, key -> new AtomicInteger(0));
        return count.incrementAndGet();
    }

    private static int decrementVxlanGpeRefCnt(final String vxlanGpePortKey, final String vppNode) {
        final ConcurrentMap<String, AtomicInteger> nodeMap = VXLAN_GPER_PORT_REF_CNT.get(vppNode);
        if (nodeMap == null) {
            return 0;
        }

        final AtomicInteger count = nodeMap.get(vxlanGpePortKey);
        if (count == null) {
            return 0;
        }
        return count.decrementAndGet();
    }

    private static void addVxlanGpePort(final DataBroker dataBroker, final IpAddress local, final IpAddress remote,
            Long vni, String vppNode, String bridgeDomainName) {
        String vxlanGpePortKey = buildVxlanGpePortKey(remote);
        LOG.info("addVxlanGpePort {} on vpp node {}", vxlanGpePortKey, vppNode);
        /* do nothing if vxlanGpePortKey has been added on vppNode */
        if (incrementVxlanGpeRefCnt(vxlanGpePortKey, vppNode) > 1) {
            return;
        }

        final VxlanGpeBuilder vxlanGpeBuilder = new VxlanGpeBuilder();

        vxlanGpeBuilder.setLocal(local);
        vxlanGpeBuilder.setRemote(remote);
        vxlanGpeBuilder.setVni(new VxlanGpeVni(vni));
        vxlanGpeBuilder.setNextProtocol(VxlanGpeNextProtocol.Nsh);
        vxlanGpeBuilder.setEncapVrfId(0L);
        vxlanGpeBuilder.setDecapVrfId(0L);

        final RoutingBuilder routingBuilder = new RoutingBuilder();
        routingBuilder.setIpv4VrfId(0L);

        final InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setName(vxlanGpePortKey);
        interfaceBuilder.setType(VxlanGpeTunnel.class);
        VppInterfaceAugmentationBuilder vppInterfaceAugmentationBuilder = new VppInterfaceAugmentationBuilder();
        vppInterfaceAugmentationBuilder.setVxlanGpe(vxlanGpeBuilder.build());
        vppInterfaceAugmentationBuilder.setRouting(routingBuilder.build());

        // Set L2 bridgedomain, is it required?
        final L2Builder l2Builder = new L2Builder();
        final BridgeBasedBuilder bridgeBasedBuilder = new BridgeBasedBuilder();
        bridgeBasedBuilder.setBridgedVirtualInterface(false);
        bridgeBasedBuilder.setSplitHorizonGroup(Short.valueOf("1"));
        bridgeBasedBuilder.setBridgeDomain(bridgeDomainName);
        l2Builder.setInterconnection(bridgeBasedBuilder.build());
        vppInterfaceAugmentationBuilder.setL2(l2Builder.build());

        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppInterfaceAugmentationBuilder.build());
        interfaceBuilder.setEnabled(true);
        interfaceBuilder.setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled);

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid = InstanceIdentifier
                .create(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceBuilder.getName()));
        wTx.put(LogicalDatastoreType.CONFIGURATION, interfaceIid, interfaceBuilder.build());
        addFuturesCallback(wTx);
    }

    public static void removeVxlanGpePort(final DataBroker dataBroker, final IpAddress local, final IpAddress remote,
            Long vni, String vppNode) {
        String interfaceKey = buildVxlanGpePortKey(remote);
        LOG.info("removeVxlanGpePort {} on vpp node {}", interfaceKey, vppNode);
        /* do nothing if interfaceKey is still used by other RSPs on vppNode */
        if (decrementVxlanGpeRefCnt(interfaceKey, vppNode) > 0) {
            return;
        }

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid = InstanceIdentifier
                .create(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceKey));
        LOG.info("removeVxlanGpePort {} on vpp node {}", interfaceKey, vppNode);
        wTx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIid);
        addFuturesCallback(wTx);
    }

    private static String buildNshEntryKey(final Long nsp, final Short nsi) {
        return "nsh_entry_" + nsp + "_" + nsi;
    }

    public static void addDummyNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode) {
        NshEntryBuilder nshEntryBuilder = new NshEntryBuilder();
        nshEntryBuilder.setVersion(Short.valueOf("0"));
        nshEntryBuilder.setLength(Short.valueOf("6"));
        nshEntryBuilder.setNsp(nsp);
        nshEntryBuilder.setNsi(nsi);
        nshEntryBuilder.setName(buildNshEntryKey(nsp, nsi));
        nshEntryBuilder.setKey(new NshEntryKey(nshEntryBuilder.getName()));
        nshEntryBuilder.setMdType(MdType1.class);
        nshEntryBuilder.setNextProtocol(Ethernet.class);
        NshMdType1AugmentBuilder nshMdType1AugmentBuilder = new NshMdType1AugmentBuilder();
        Long c1 = 0L;
        Long c2 = 0L;
        Long c3 = 0L;
        Long c4 = 0L;
        nshMdType1AugmentBuilder.setC1(c1);
        nshMdType1AugmentBuilder.setC2(c2);
        nshMdType1AugmentBuilder.setC3(c3);
        nshMdType1AugmentBuilder.setC4(c4);
        nshEntryBuilder.addAugmentation(NshMdType1Augment.class, nshMdType1AugmentBuilder.build());

        NshEntriesBuilder nshEntriesBuilder = new NshEntriesBuilder();
        List<NshEntry> nshEntryList = new ArrayList<>();
        nshEntryList.add(nshEntryBuilder.build());
        nshEntriesBuilder.setNshEntry(nshEntryList);

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshEntries> nshEntriesIid = InstanceIdentifier.create(VppNsh.class)
                .child(NshEntries.class);
        wTx.put(LogicalDatastoreType.CONFIGURATION, nshEntriesIid, nshEntriesBuilder.build());
        addFuturesCallback(wTx);
    }

    public static void addNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode) {
        NshEntryBuilder nshEntryBuilder = new NshEntryBuilder();
        nshEntryBuilder.setVersion(Short.valueOf("0"));
        nshEntryBuilder.setLength(Short.valueOf("6"));
        nshEntryBuilder.setNsp(nsp);
        nshEntryBuilder.setNsi(nsi);
        nshEntryBuilder.setName(buildNshEntryKey(nsp, nsi));
        nshEntryBuilder.setKey(new NshEntryKey(nshEntryBuilder.getName()));
        nshEntryBuilder.setMdType(MdType1.class);
        nshEntryBuilder.setNextProtocol(Ethernet.class);
        NshMdType1AugmentBuilder nshMdType1AugmentBuilder = new NshMdType1AugmentBuilder();
        Long c1 = 0L;
        Long c2 = 0L;
        Long c3 = 0L;
        Long c4 = 0L;
        nshMdType1AugmentBuilder.setC1(c1);
        nshMdType1AugmentBuilder.setC2(c2);
        nshMdType1AugmentBuilder.setC3(c3);
        nshMdType1AugmentBuilder.setC4(c4);
        nshEntryBuilder.addAugmentation(NshMdType1Augment.class, nshMdType1AugmentBuilder.build());
        NshEntry nshEntry = nshEntryBuilder.build();

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshEntry> nshEntryIid = InstanceIdentifier.create(VppNsh.class).child(NshEntries.class)
                .child(NshEntry.class, nshEntry.getKey());
        wTx.put(LogicalDatastoreType.CONFIGURATION, nshEntryIid, nshEntry);
        addFuturesCallback(wTx);
    }

    public static void removeNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        String nshEntryKey = buildNshEntryKey(nsp, nsi);
        final InstanceIdentifier<NshEntry> nshEntryIid = InstanceIdentifier.create(VppNsh.class).child(NshEntries.class)
                .child(NshEntry.class, new NshEntryKey(nshEntryKey));
        LOG.info("removeNshEntry {} on vpp node {}", nshEntryKey, vppNode);
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nshEntryIid);
        addFuturesCallback(wTx);
    }

    private static String buildNshMapKey(final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi) {
        return "nsh_map_" + nsp + "_" + nsi + "_to_" + mappedNsp + "_" + mappedNsi;
    }

    private static NshMapBuilder buildNshMapBuilder(final Long nsp, final Short nsi, final Long mappedNsp,
            final Short mappedNsi, String encapIfName) {
        NshMapBuilder nshMapBuilder = new NshMapBuilder();
        nshMapBuilder.setNsp(nsp);
        nshMapBuilder.setNsi(nsi);
        nshMapBuilder.setMappedNsp(mappedNsp);
        nshMapBuilder.setMappedNsi(mappedNsi);
        nshMapBuilder.setName(buildNshMapKey(nsp, nsi, mappedNsp, mappedNsi));
        nshMapBuilder.setKey(new NshMapKey(nshMapBuilder.getName()));
        if (encapIfName != null) {
            nshMapBuilder.setEncapType(VxlanGpe.class);
            nshMapBuilder.setEncapIfName(encapIfName);
        } else {
            nshMapBuilder.setEncapType(None.class);
        }
        return nshMapBuilder;
    }

    private static void writeNshMap(final DataBroker dataBroker, NshMap nshMap, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshMap> nshMapIid = InstanceIdentifier.create(VppNsh.class).child(NshMaps.class)
                .child(NshMap.class, nshMap.getKey());
        wTx.put(LogicalDatastoreType.CONFIGURATION, nshMapIid, nshMap);
        addFuturesCallback(wTx);
    }

    public static void addDummyNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi,
            final Long mappedNsp, final Short mappedNsi, String encapIfName, String vppNode) {
        NshMapBuilder nshMapBuilder = buildNshMapBuilder(nsp, nsi, mappedNsp, mappedNsi, encapIfName);
        nshMapBuilder.setNshAction(Swap.class);

        List<NshMap> nshMapList = new ArrayList<>();
        nshMapList.add(nshMapBuilder.build());

        NshMapsBuilder nshMapsBuilder = new NshMapsBuilder();
        nshMapsBuilder.setNshMap(nshMapList);

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshMaps> nshMapsIid = InstanceIdentifier.create(VppNsh.class).child(NshMaps.class);
        wTx.put(LogicalDatastoreType.CONFIGURATION, nshMapsIid, nshMapsBuilder.build());
        addFuturesCallback(wTx);
    }

    private static void addNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi, final Long mappedNsp,
            final Short mappedNsi, String encapIfName, String vppNode) {
        NshMapBuilder nshMapBuilder = buildNshMapBuilder(nsp, nsi, mappedNsp, mappedNsi, encapIfName);
        nshMapBuilder.setNshAction(Swap.class);
        writeNshMap(dataBroker, nshMapBuilder.build(), vppNode);
    }

    private static void addNshMapWithPush(final DataBroker dataBroker, final Long nsp, final Short nsi,
            final Long mappedNsp, final Short mappedNsi, String encapIfName, String vppNode) {
        NshMapBuilder nshMapBuilder = buildNshMapBuilder(nsp, nsi, mappedNsp, mappedNsi, encapIfName);
        nshMapBuilder.setNshAction(Push.class);
        writeNshMap(dataBroker, nshMapBuilder.build(), vppNode);
    }

    public static void addNshMapWithPop(final DataBroker dataBroker, final Long nsp, final Short nsi,
            final Long mappedNsp, final Short mappedNsi, String encapIfName, String vppNode) {
        NshMapBuilder nshMapBuilder = buildNshMapBuilder(nsp, nsi, mappedNsp, mappedNsi, encapIfName);
        nshMapBuilder.setNshAction(Pop.class);
        writeNshMap(dataBroker, nshMapBuilder.build(), vppNode);
    }

    public static void removeNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi, final Long mappedNsp,
            final Short mappedNsi, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        String nshMapKey = buildNshMapKey(nsp, nsi, mappedNsp, mappedNsi);
        final InstanceIdentifier<NshMap> nshMapIid = InstanceIdentifier.create(VppNsh.class).child(NshMaps.class)
                .child(NshMap.class, new NshMapKey(nshMapKey));
        LOG.info("removeNshMap {} on vpp node {}", nshMapKey, vppNode);
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nshMapIid);
        addFuturesCallback(wTx);
    }

    public static boolean configureVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName,
            String bridgeDomainName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp,
            final Short nsi) {
        Long vni = 0L; // SFC classifier set it to 0, so always use 0

        addVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue(), bridgeDomainName); // SFF<->SF
        addNshEntry(dataBroker, nsp, nsi, sffName.getValue()); // To Next Hop
        addNshMap(dataBroker, nsp, nsi, nsp, nsi, buildVxlanGpePortKey(remoteIp), sffName.getValue());

        return true;
    }

    public static boolean removeVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress localIp,
            final IpAddress remoteIp, final Long nsp, final Short nsi) {
        Long vni = 0L; // SFC classifier set it to 0, so always use 0

        removeNshMap(dataBroker, nsp, nsi, nsp, nsi, sffName.getValue());
        removeNshEntry(dataBroker, nsp, nsi, sffName.getValue()); // To SF
        removeVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue()); // SFF<->SF

        return true;
    }

    private static Integer getNextTableIndex(String vppNode) {
        return TABLE_INDICE.computeIfAbsent(vppNode, key -> Integer.valueOf(0));
    }

    public static Integer increaseNextTableIndex(String vppNode) {
        getNextTableIndex(vppNode);
        return TABLE_INDICE.computeIfPresent(vppNode, (key, oldValue) -> Integer.valueOf(oldValue + 1));
    }

    public static String buildClassifyTableKey(final Integer tableIndex) {
        return "table" + tableIndex;
    }

    private static ClassifyTableBuilder buildClassifyTable(String classifyTableKey, String nextTableKey,
            final HexString mask) {
        ClassifyTableBuilder classifyTableBuilder = new ClassifyTableBuilder();
        classifyTableBuilder.setName(classifyTableKey);
        if (nextTableKey != null) {
            classifyTableBuilder.setNextTable(nextTableKey);
        }
        classifyTableBuilder.setClassifierNode(new VppNodeName("l2-input-classify"));
        classifyTableBuilder.setNbuckets(2L);
        classifyTableBuilder.setMemorySize(104857L);
        classifyTableBuilder.setMissNext(new VppNode(PacketHandlingAction.Deny));
        classifyTableBuilder.setMask(mask);
        return classifyTableBuilder;
    }

    public static void addClassifyTable(final DataBroker dataBroker, ClassifyTable classifyTable, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();

        if (FIRST_TABLE.putIfAbsent(vppNode, classifyTable.getName()) == null) {
            VppClassifierBuilder vppClassifierBuilder = new VppClassifierBuilder();
            List<ClassifyTable> classifyTableList = new ArrayList<>();
            classifyTableList.add(classifyTable);
            vppClassifierBuilder.setClassifyTable(classifyTableList);
            LOG.info("addClassifyTable: {}", vppClassifierBuilder.build());

            final InstanceIdentifier<VppClassifier> vppClassifierIid = InstanceIdentifier.create(VppClassifier.class);
            wTx.put(LogicalDatastoreType.CONFIGURATION, vppClassifierIid, vppClassifierBuilder.build());
        } else {
            final InstanceIdentifier<ClassifyTable> classifyTableIid = InstanceIdentifier.create(VppClassifier.class)
                    .child(ClassifyTable.class, classifyTable.getKey());
            wTx.put(LogicalDatastoreType.CONFIGURATION, classifyTableIid, classifyTable);
            LOG.info("addClassifyTable: {}", classifyTable);
        }
        addFuturesCallback(wTx);
    }

    private static void removeClassifyTable(final DataBroker dataBroker, final String classifyTableKey,
            String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<ClassifyTable> classifyTableIid = InstanceIdentifier.create(VppClassifier.class)
                .child(ClassifyTable.class, new ClassifyTableKey(classifyTableKey));
        LOG.info("removeClassifyTable on vpp node {}: table: {}", vppNode, classifyTableKey);
        wTx.delete(LogicalDatastoreType.CONFIGURATION, classifyTableIid);
        addFuturesCallback(wTx);
    }

    private static ClassifySessionBuilder buildClassifySession(final String classifyTableKey, Long nsp, Short nsi,
            final HexString match) {
        ClassifySessionBuilder classifySessionBuilder = new ClassifySessionBuilder();
        classifySessionBuilder.setMatch(match);
        classifySessionBuilder.setHitNext(new VppNode(new VppNodeName("nsh-classifier")));
        Long opaqueIndexValue = Long.valueOf(nsp.longValue() << 8 | nsi.intValue());
        classifySessionBuilder.setOpaqueIndex(new OpaqueIndex(opaqueIndexValue));
        return classifySessionBuilder;
    }

    private static void removeClassifySession(final DataBroker dataBroker, final String classifyTableKey,
            HexString match, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<ClassifySession> classifySessionIid = InstanceIdentifier.create(VppClassifier.class)
                .child(ClassifyTable.class, new ClassifyTableKey(classifyTableKey))
                .child(ClassifySession.class, new ClassifySessionKey(match));
        LOG.info("removeClassifySession on vpp node {}: table: {}, session: {}", vppNode, classifyTableKey, match);
        wTx.delete(LogicalDatastoreType.CONFIGURATION, classifySessionIid);
        addFuturesCallback(wTx);
    }

    public static void enableIngressAcl(final DataBroker dataBroker, final String interfaceName,
            final String classifyTableKey, String vppNode) {
        IngressBuilder ingressBuilder = new IngressBuilder();
        Ip4Acl acl = new Ip4AclBuilder().setClassifyTable(classifyTableKey).build();
        ingressBuilder.setIp4Acl(acl);
        Ingress ingress = ingressBuilder.build();

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<Ingress> ingressIid = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName)).augmentation(VppInterfaceAugmentation.class)
                .child(Acl.class).child(Ingress.class);
        wTx.put(LogicalDatastoreType.CONFIGURATION, ingressIid, ingress);
        addFuturesCallback(wTx);
    }

    public static void disableIngressAcl(final DataBroker dataBroker, final String interfaceName,
            final String classifyTableKey, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<Ingress> ingressIid = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName)).augmentation(VppInterfaceAugmentation.class)
                .child(Acl.class).child(Ingress.class);
        wTx.delete(LogicalDatastoreType.CONFIGURATION, ingressIid);
        addFuturesCallback(wTx);
    }

    private static void saveClassifyTableKey(String vppNode, String rsp, String classifyTableKey) {
        String rspKey = vppNode + "_" + rsp;
        List<String> tblIdList = RSP_TABLE_ID_LIST.computeIfAbsent(rspKey, key -> new ArrayList<>());

        synchronized (tblIdList) {
            tblIdList.add(classifyTableKey);
        }
    }

    public static String getSavedClassifyTableKey(String vppNode, String rsp, int index) {
        String rspKey = vppNode + "_" + rsp;

        List<String> tblIdList = RSP_TABLE_ID_LIST.get(rspKey);
        if (tblIdList == null) {
            return null;
        }

        synchronized (tblIdList) {
            if (tblIdList.size() <= index) {
                return null;
            }

            return tblIdList.get(index);
        }
    }

    public static ClassifyTableBuilder buildVppClassifyTable(SffName sffName, String rsp, HexString mask,
            boolean hasNext) {
        Integer index = getNextTableIndex(sffName.getValue());
        String classifyTableKey = buildClassifyTableKey(index);
        saveClassifyTableKey(sffName.getValue(), rsp, classifyTableKey);
        String nextTableKey = null;
        if (hasNext) {
            nextTableKey = buildClassifyTableKey(index + 1);
        }
        return buildClassifyTable(classifyTableKey, nextTableKey, mask);
    }

    public static ClassifySessionBuilder buildVppClassifySession(ClassifyTableBuilder classifyTableBuilder,
            HexString match, Long nsp, Short nsi) {
        return buildClassifySession(classifyTableBuilder.getName(), nsp, nsi, match);
    }

    public static boolean configureVppClassifier(DataBroker dataBroker, SffName sffName,
            List<ClassifyTableBuilder> classifyTableList, List<ClassifySessionBuilder> classifySessionList) {
        for (int i = classifyTableList.size() - 1; i >= 0; i--) {
            ClassifyTableBuilder classifyTableBuilder = classifyTableList.get(i);
            ClassifySessionBuilder classifySessionBuilder = classifySessionList.get(i);
            List<ClassifySession> sessionList = new ArrayList<>();
            sessionList.add(classifySessionBuilder.build());
            classifyTableBuilder.setClassifySession(sessionList);
            addClassifyTable(dataBroker, classifyTableBuilder.build(), sffName.getValue());
        }
        return true;
    }

    public static boolean removeVppClassifier(DataBroker dataBroker, SffName sffName, List<String> tableKeyList,
            List<HexString> matchList) {
        for (int i = 0; i < tableKeyList.size(); i++) {
            removeClassifySession(dataBroker, tableKeyList.get(i), matchList.get(i), sffName.getValue());
            removeClassifyTable(dataBroker, tableKeyList.get(i), sffName.getValue());
        }
        return true;
    }

    public static boolean configureClassifierVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName,
            String bridgeDomainName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp,
            final Short nsi) {
        Long vni = 0L; // SFC classifier set it to 0, so always use 0

        addVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue(), bridgeDomainName); // SFF<->SF
        addNshEntry(dataBroker, nsp, nsi, sffName.getValue()); // To Next Hop
        addNshMapWithPush(dataBroker, nsp, nsi, nsp, nsi, buildVxlanGpePortKey(remoteIp), sffName.getValue());

        return true;
    }

    public static boolean removeClassifierVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName,
            String bridgeDomainName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp,
            final Short nsi) {
        Long vni = 0L; // SFC classifier set it to 0, so always use 0

        removeNshMap(dataBroker, nsp, nsi, nsp, nsi, sffName.getValue());
        removeNshEntry(dataBroker, nsp, nsi, sffName.getValue()); // To SFF
        removeVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue()); // Classifier<->SFF

        return true;
    }
}
