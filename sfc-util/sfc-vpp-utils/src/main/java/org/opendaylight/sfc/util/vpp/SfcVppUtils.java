/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.util.vpp;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.SfLocatorProxyAugmentation;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sf.proxy.rev160125.proxy.ProxyDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1AugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SfcVppUtils {
    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_IID = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
            .build();

    public static DataBroker getSffMountpoint(MountPointService mountService, SffName sffName) {
        final NodeId nodeId = new NodeId(sffName.getValue());
        InstanceIdentifier<Node> netconfNodeIid = NETCONF_TOPOLOGY_IID.child(Node.class, new NodeKey(new NodeId(nodeId)));
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
        SffDataPlaneLocator sffDpl = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderDataPlaneLocator(sffName, sffDplName);
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

    private static IpAddress getSfDplIp(SfName sfName, SfDataPlaneLocatorName sfDplName) {
        IpAddress ip = null;
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        if (serviceFunction == null) {
            return null;
        }

        List<SfDataPlaneLocator> sdDplList = serviceFunction.getSfDataPlaneLocator();
        SfDataPlaneLocator sfDataPlaneLocator = null;
        for (SfDataPlaneLocator sfDpl: sdDplList) {
            if (sfDpl.getName().equals(sfDplName)) {
                sfDataPlaneLocator = sfDpl;
                break;
            }
        }

        if (sfDataPlaneLocator == null) {
            return null;
        }

        LocatorType locatorType = sfDataPlaneLocator.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }

        return ip;
    }

    public static List<IpAddress> getSffSfIps(final SffName sffName, final SfName sfName) {
        List<IpAddress> ipList = new ArrayList<>();
        ServiceFunctionDictionary sfDictionary;
        SfDataPlaneLocatorName sfDplName;
        SffDataPlaneLocatorName sffDplName;
        IpAddress localIp;
        IpAddress remoteIp;

        sfDictionary = getSfDictionary(sffName, sfName);
        if (sfDictionary == null) {
            return null;
        }

        sfDplName = sfDictionary.getSffSfDataPlaneLocator().getSfDplName();
        sffDplName = sfDictionary.getSffSfDataPlaneLocator().getSffDplName();

        localIp = getSffDplIp(sffName, sffDplName);
        if (localIp == null) {
            return null;
        }
        ipList.add(localIp);

        remoteIp = getSfDplIp(sfName, sfDplName);
        if (remoteIp == null) {
            return null;
        }
        ipList.add(remoteIp);
        return ipList;
    }

    public static SfLocatorProxyAugmentation getSfDplProxyAugmentation(final ServiceFunction sf, final SffName sffName) {
        ServiceFunctionDictionary sfDictionary;
        SfDataPlaneLocatorName sfDplName;

        sfDictionary = getSfDictionary(sffName, sf.getName());
        if (sfDictionary == null) {
            return null;
        }

        sfDplName = sfDictionary.getSffSfDataPlaneLocator().getSfDplName();

        List<SfDataPlaneLocator> sdDplList = sf.getSfDataPlaneLocator();
        SfDataPlaneLocator sfDataPlaneLocator = null;
        for (SfDataPlaneLocator sfDpl: sdDplList) {
            if (sfDpl.getName().equals(sfDplName)) {
                sfDataPlaneLocator = sfDpl;
                break;
            }
        }

        if (sfDataPlaneLocator == null) {
            return null;
        }

        return sfDataPlaneLocator.getAugmentation(SfLocatorProxyAugmentation.class);
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

    public static void addBridgeDomain(final DataBroker dataBroker, String bridgeDomainName, String vppNode) {
        InstanceIdentifier<BridgeDomain> bridgeDomainIId =
            InstanceIdentifier.create(Vpp.class)
                .child(BridgeDomains.class)
                .child(BridgeDomain.class, new BridgeDomainKey(bridgeDomainName));

        BridgeDomainBuilder bdBuilder = new BridgeDomainBuilder();
        bdBuilder.setName(bridgeDomainName);
        bdBuilder.setFlood(true);
        bdBuilder.setForward(true);
        bdBuilder.setLearn(true);
        bdBuilder.setUnknownUnicastFlood(true);
        bdBuilder.setArpTermination(false);

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, bridgeDomainIId, bdBuilder.build());

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static String buildVxlanGpePortKey(final IpAddress remote) {
        return new String("vxlanGpeTun" + "_" + remote.getIpv4Address().getValue());
    }

    private static void addVxlanGpePort(final DataBroker dataBroker, final IpAddress local, final IpAddress remote, Long vni, String vppNode, String bridgeDomainName)
    {
        final VxlanGpeBuilder vxlanGpeBuilder = new VxlanGpeBuilder();

        vxlanGpeBuilder.setLocal(local);
        vxlanGpeBuilder.setRemote(remote);
        vxlanGpeBuilder.setVni(new VxlanGpeVni(vni));
        vxlanGpeBuilder.setNextProtocol(VxlanGpeNextProtocol.Nsh);
        vxlanGpeBuilder.setEncapVrfId(0L);
        vxlanGpeBuilder.setDecapVrfId(0L);

        final RoutingBuilder routingBuilder = new RoutingBuilder();
        routingBuilder.setVrfId(0L);

        final InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setName(buildVxlanGpePortKey(remote));
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
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceBuilder.getName()));
        wTx.put(LogicalDatastoreType.CONFIGURATION, interfaceIid, interfaceBuilder.build());

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    public static void removeVxlanGpePort(final DataBroker dataBroker, final IpAddress local, final IpAddress remote, Long vni, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(buildVxlanGpePortKey(remote)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIid);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static String buildVxlanPortKey(final IpAddress dstIp) {
        return new String("vxlanTun" + "_" + dstIp.getIpv4Address().getValue());
    }

    private static void addVxlanPort(final DataBroker dataBroker, final IpAddress srcIp, final IpAddress dstIp, Long vni, String vppNode, String bridgeDomainName)
    {
        final VxlanBuilder vxlanBuilder = new VxlanBuilder();

        vxlanBuilder.setSrc(srcIp);
        vxlanBuilder.setDst(dstIp);
        vxlanBuilder.setVni(new VxlanVni(vni));
        vxlanBuilder.setEncapVrfId(0L);

        final InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setName(buildVxlanPortKey(dstIp));
        interfaceBuilder.setType(VxlanTunnel.class);
        VppInterfaceAugmentationBuilder vppInterfaceAugmentationBuilder = new VppInterfaceAugmentationBuilder();
        vppInterfaceAugmentationBuilder.setVxlan(vxlanBuilder.build());

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

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceBuilder.getName()));
        wTx.put(LogicalDatastoreType.CONFIGURATION, interfaceIid, interfaceBuilder.build());

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static void removeVxlanPort(final DataBroker dataBroker, final IpAddress srcIp, final IpAddress dstIp, Long vni, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(buildVxlanPortKey(dstIp)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIid);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static String buildNshEntryKey(final Long nsp, final Short nsi) {
        return new String("nsh_entry_" + nsp.toString() + "_" + nsi.toString());
    }

    private static void addNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode)
    {
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
        final InstanceIdentifier<NshEntry> nshEntryIid
                    = InstanceIdentifier.create(VppNsh.class).child(NshEntries.class).child(NshEntry.class, nshEntry.getKey());
        wTx.put(LogicalDatastoreType.CONFIGURATION, nshEntryIid, nshEntry);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static void removeNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshEntry> nshEntryIid
                    = InstanceIdentifier.create(NshEntries.class).child(NshEntry.class, new NshEntryKey(buildNshEntryKey(nsp, nsi)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nshEntryIid);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static String buildNshMapKey(final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi) {
        return new String("nsh_map_" + nsp.toString() + "_" + nsi.toString() + "_to_" + mappedNsp.toString() + "_" + mappedNsi.toString());
    }

    private static void addNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi, String encapIfName, String vppNode)
    {
        final NshMapBuilder nshMapBuilder = new NshMapBuilder();
        nshMapBuilder.setNsp(nsp);
        nshMapBuilder.setNsi(nsi);
        nshMapBuilder.setMappedNsp(mappedNsp);
        nshMapBuilder.setMappedNsi(mappedNsi);
        nshMapBuilder.setName(buildNshMapKey(nsp, nsi, mappedNsp, mappedNsi));
        nshMapBuilder.setKey(new NshMapKey(nshMapBuilder.getName()));
        nshMapBuilder.setEncapType(VxlanGpe.class);
        nshMapBuilder.setEncapIfName(encapIfName);
        NshMap nshMap = nshMapBuilder.build();

        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshMap> nshMapIid
                    = InstanceIdentifier.create(VppNsh.class).child(NshMaps.class).child(NshMap.class, nshMap.getKey());
        wTx.put(LogicalDatastoreType.CONFIGURATION, nshMapIid, nshMap);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    private static void removeNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshMap> nshMapIid
                    = InstanceIdentifier.create(NshMaps.class).child(NshMap.class, new NshMapKey(buildNshMapKey(nsp, nsi, mappedNsp, mappedNsi)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nshMapIid);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
            }
        });
    }

    public static boolean configureVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName, String bridgeDomainName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp, final Short nsi) {
        Long vni = 0L; // SFC classifier set it to 0, so always use 0

        addVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue(), bridgeDomainName); //SFF<->SF
        addNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To Next Hop
        addNshMap(dataBroker, nsp, nsi, nsp, nsi, buildVxlanGpePortKey(remoteIp), sffName.getValue());

        return true;
    }

    public static boolean removeVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp, final Short nsi) {
        Short nextNsi = nsi;
        nextNsi--;
        Long vni = 0L; // SFC classifier set it to 0, so always use 0
        removeVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue()); //SFF<->SF
        removeNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To SF
        removeNshEntry(dataBroker, nsp, nextNsi, sffName.getValue()); //From SF
        removeNshMap(dataBroker, nsp, nsi, nsp, nextNsi, sffName.getValue());
        return true;
    }

    public static boolean configureVxlanNsh(final DataBroker dataBroker, final SffName sffName, String bridgeDomainName, final IpAddress srcIp, final IpAddress dstIp, final Long nsp, final Short nsi) {
        Long vni = 0L; // SFC classifier set it to 0, so always use 0

        addVxlanPort(dataBroker, srcIp, dstIp, vni, sffName.getValue(), bridgeDomainName); //SFF<->SF
        addNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To Next Hop
        addNshMap(dataBroker, nsp, nsi, nsp, nsi, buildVxlanPortKey(dstIp), sffName.getValue());

        return true;
    }

    public static boolean removeVxlanNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress srcIp, final IpAddress dstIp, final Long nsp, final Short nsi) {
        Short nextNsi = nsi;
        nextNsi--;
        Long vni = 0L; // SFC classifier set it to 0, so always use 0
        removeVxlanPort(dataBroker, srcIp, dstIp, vni, sffName.getValue()); //SFF<->SF
        removeNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To SF
        removeNshEntry(dataBroker, nsp, nextNsi, sffName.getValue()); //From SF
        removeNshMap(dataBroker, nsp, nsi, nsp, nextNsi, sffName.getValue());
        return true;
    }

}
