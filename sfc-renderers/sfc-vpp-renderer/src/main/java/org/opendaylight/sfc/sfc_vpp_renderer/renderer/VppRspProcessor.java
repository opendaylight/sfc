/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_vpp_renderer.renderer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.sfc_vpp_renderer.listener.RenderedPathListener;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VppRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VppRspProcessor.class);

    private final DataBroker dataBroker;
    private final VppNodeManager nodeManager;
    private final RenderedPathListener rspListener;
    private static final String SFC_BD_NAME = new String("SFCVPP");
    private final Map<String, String> bridgeDomainCreated = new HashMap<>();

    public VppRspProcessor(DataBroker dataBroker, VppNodeManager nodeManager) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.nodeManager = Preconditions.checkNotNull(nodeManager);
        // Register RSP listener
        rspListener = new RenderedPathListener(dataBroker, this);
    }

    public void updateRsp(RenderedServicePath renderedServicePath) {
        Preconditions.checkNotNull(renderedServicePath);
        Long pathId = renderedServicePath.getPathId();
        Short serviceIndex = renderedServicePath.getStartingIndex();
        DataBroker previousMountPoint = null;
        DataBroker currentMountpoint = null;
        SffName previousSffName = null;
        SffName currentSffName = null;
        SfName sfName;
        List<IpAddress> ipList = null;
        IpAddress localIp = null;
        IpAddress remoteIp;
        IpAddress preLocalIp = null;
        boolean ret = false;

        if (renderedServicePath.getRenderedServicePathHop() == null ||
                renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            LOG.warn("Rendered path {} does not contain any hop", renderedServicePath.getName().getValue());
            return;
        }
        Iterator<RenderedServicePathHop> rspHopIterator = renderedServicePath.getRenderedServicePathHop()
                .iterator();

        while (rspHopIterator.hasNext()) {
            RenderedServicePathHop hop = rspHopIterator.next();
            previousSffName = currentSffName;
            previousMountPoint = currentMountpoint;
            preLocalIp = localIp;
            currentSffName = hop.getServiceFunctionForwarder();
            currentMountpoint = getSffMountpoint(currentSffName);
            if (currentMountpoint == null) {
                LOG.error("Resolving of RSP {} failed, mountpoint for SFF {} is null", renderedServicePath.getName()
                    .getValue(), currentSffName.getValue());
                return;
            }

            sfName = hop.getServiceFunctionName();
            serviceIndex = hop.getServiceIndex();
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
            if (serviceFunction == null) {
                LOG.error("Service function {} not present in datastore", sfName.getValue());
                return;
            }
            ipList = getSffSfIps(currentSffName, sfName);
            if ((ipList == null) || ipList.isEmpty()) {
                LOG.error("failed to get IP for DPL for SFF {} in RSP {}", currentSffName.getValue(), renderedServicePath.getName().getValue());
                return;
            }
            localIp = ipList.get(0);
            remoteIp = ipList.get(1);

            /* Create BridgeDomain */
            if (!bridgeDomainCreated.containsKey(currentSffName.getValue())) {
                addBridgeDomain(currentMountpoint, SFC_BD_NAME, currentSffName.getValue());
                bridgeDomainCreated.put(currentSffName.getValue(), SFC_BD_NAME);
            }

            SfLocatorProxyAugmentation sfDplProxyAug = getSfDplProxyAugmentation(serviceFunction, currentSffName);
            if (sfDplProxyAug == null) {
                /* NSH-aware SF */
                ret = configureVxlanGpeNsh(currentMountpoint, currentSffName, localIp, remoteIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to configure VxLAN-gpe and NSH for RSP {} in SFF {} for SF hop", renderedServicePath.getName().getValue(), currentSffName.getValue());
                    return;
                }
            } else {
                /* SF has NSH Proxy, i.e. NSH-unaware SF,
                 * so create VxLAN port for NSH proxy.
                 */
                remoteIp = getSfProxyDplIp(sfDplProxyAug);
                if (remoteIp == null) {
                    LOG.error("failed to get IP for SF DPL for SF {} in RSP {}", sfName.getValue(), renderedServicePath.getName().getValue());
                    return;
                }
                ret = configureVxlanNsh(currentMountpoint, currentSffName, localIp, remoteIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to configure VxLAN and NSH for RSP {} in SFF {} for SF hop", renderedServicePath.getName().getValue(), currentSffName.getValue());
                    return;
                }
            }

            if ((previousSffName != null) && (!previousSffName.equals(currentSffName))) {
                //previous SFF <-> current SFF
                ret = configureVxlanGpeNsh(previousMountPoint, previousSffName, preLocalIp, localIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to configure VxLAN-gpe and NSH for RSP {} in SFF {} for SFF hop", renderedServicePath.getName().getValue(), previousSffName.getValue());
                    return;
                }
            }
        }

        /* TODO: Configure VxlanGpeNsh for last hop to classifier */
    }

    public void deleteRsp(RenderedServicePath renderedServicePath) {
        boolean ret = false;
        Preconditions.checkNotNull(renderedServicePath);
        Long pathId = renderedServicePath.getPathId();
        Short serviceIndex = renderedServicePath.getStartingIndex();
        DataBroker previousMountPoint = null;
        DataBroker currentMountpoint = null;
        SffName previousSffName = null;
        SffName currentSffName = null;
        SfName sfName;
        List<IpAddress> ipList = null;
        IpAddress localIp = null;
        IpAddress remoteIp;
        IpAddress preLocalIp = null;

        if (renderedServicePath.getRenderedServicePathHop() == null ||
                renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            LOG.warn("Rendered path {} does not contain any hop", renderedServicePath.getName().getValue());
            return;
        }
        Iterator<RenderedServicePathHop> rspHopIterator = renderedServicePath.getRenderedServicePathHop()
                .iterator();

        while (rspHopIterator.hasNext()) {
            RenderedServicePathHop hop = rspHopIterator.next();
            previousSffName = currentSffName;
            previousMountPoint = currentMountpoint;
            preLocalIp = localIp;
            currentSffName = hop.getServiceFunctionForwarder();
            currentMountpoint = getSffMountpoint(currentSffName);
            if (currentMountpoint == null) {
                LOG.error("Resolving of RSP {} failed, mountpoint for SFF {} is null", renderedServicePath.getName()
                    .getValue(), currentSffName.getValue());
                return;
            }

            sfName = hop.getServiceFunctionName();
            serviceIndex = hop.getServiceIndex();
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
            if (serviceFunction == null) {
                LOG.error("Service function {} not present in datastore", sfName.getValue());
                return;
            }
            ipList = getSffSfIps(currentSffName, sfName);
            if ((ipList == null) || ipList.isEmpty()) {
                LOG.error("failed to get IP for DPL for SFF {} in RSP {}", currentSffName.getValue(), renderedServicePath.getName().getValue());
                return;
            }
            localIp = ipList.get(0);
            remoteIp = ipList.get(1);

            SfLocatorProxyAugmentation sfDplProxyAug = getSfDplProxyAugmentation(serviceFunction, currentSffName);
            if (sfDplProxyAug == null) {
                ret = removeVxlanGpeNsh(currentMountpoint, currentSffName, localIp, remoteIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to remove VxLAN-gpe and NSH for RSP {} in SFF {}", renderedServicePath.getName().getValue(), currentSffName.getValue());
                    return;
                }
            } else {
                remoteIp = getSfProxyDplIp(sfDplProxyAug);
                if (remoteIp == null) {
                    LOG.error("failed to get IP for SF DPL for SFF {} in RSP {}", currentSffName.getValue(), renderedServicePath.getName().getValue());
                    return;
                }
                ret = removeVxlanNsh(currentMountpoint, currentSffName, localIp, remoteIp, pathId, serviceIndex);
                if (!ret) {
                    LOG.error("failed to remove VxLAN and NSH for RSP {} in SFF {}", renderedServicePath.getName().getValue(), currentSffName.getValue());
                    return;
                }
            }

            if ((previousSffName != null) && (!previousSffName.equals(currentSffName))) {
                removeVxlanGpePort(previousMountPoint, preLocalIp, localIp, Long.valueOf(0), previousSffName.getValue()); //previous SFF <-> current SFF
                removeVxlanGpePort(currentMountpoint, localIp, preLocalIp, Long.valueOf(0), currentSffName.getValue());  //current SFF <-> previous SFF
            }
        }
    }

    private DataBroker getSffMountpoint(SffName sffName) {
        // Read SFF from Controller CONF
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder sfcForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        if (sfcForwarder == null) {
            LOG.error("SFF name {} not found in data store", sffName.getValue());
            return null;
        }
        IpAddress sffMgmtIp = sfcForwarder.getIpMgmtAddress();
        if (sffMgmtIp == null) {
            LOG.error("Unable to obtain management IP for SFF {}", sffName.getValue());
            return null;
        }
        return nodeManager.getMountpointFromIpAddress(new IpAddress(new Ipv4Address(sffMgmtIp.getIpv4Address()
                .getValue())));
    }

    private ServiceFunctionDictionary getSfDictionary(SffName sffName, SfName sfName) {
        ServiceFunctionDictionary sfDictionary = null;
        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        if (sff == null) {
            LOG.error("Can not find SFF {} in data store", sffName.getValue());
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

    private IpAddress getSffDplIp(SffName sffName, SffDataPlaneLocatorName sffDplName) {
        IpAddress ip = null;
        SffDataPlaneLocator sffDpl = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderDataPlaneLocator(sffName, sffDplName);
        if (sffDpl == null) {
            LOG.error("Can not find DPL {} in SFF {} in data store", sffDplName.getValue(), sffName.getValue());
            return null;
        }

        DataPlaneLocator dpl = sffDpl.getDataPlaneLocator();
        if (dpl == null) {
            LOG.error("Can not find DataPlaneLocator in SffDpl {} in SFF {} in data store", sffDplName.getValue(), sffName.getValue());
            return null;
        }

        LocatorType locatorType = dpl.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }

        return ip;
    }

    private IpAddress getSfDplIp(SfName sfName, SfDataPlaneLocatorName sfDplName) {
        IpAddress ip = null;
        ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        if (serviceFunction == null) {
            LOG.error("Service function {} not present in datastore", sfName.getValue());
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
            LOG.error("Can not find DPL {} in SF {} in data store", sfDplName.getValue(), sfName.getValue());
            return null;
        }

        LocatorType locatorType = sfDataPlaneLocator.getLocatorType();
        if (locatorType instanceof Ip) {
            IpPortLocator ipPortLocator = (IpPortLocator) locatorType;
            ip = ipPortLocator.getIp();
        }

        return ip;
    }

    private List<IpAddress> getSffSfIps(final SffName sffName, final SfName sfName) {
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

    private SfLocatorProxyAugmentation getSfDplProxyAugmentation(final ServiceFunction sf, final SffName sffName) {
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

    private IpAddress getSfProxyDplIp(SfLocatorProxyAugmentation augment) {
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

    private void addBridgeDomain(final DataBroker dataBroker, String bridgeDomainName, String vppNode) {
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

        LOG.debug("Submitting new bridge domain {} to config store...", bridgeDomainName);

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Writing bridge domain {} to {} finished successfully.", bridgeDomainName, vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Writing bridge domain {} to {} failed: {}", bridgeDomainName, vppNode, t);
            }
        });
    }

    private String buildVxlanGpePortKey(final IpAddress remote) {
        return new String("vxlanGpeTun" + "_" + remote.getIpv4Address().getValue());
    }

    private void addVxlanGpePort(final DataBroker dataBroker, final IpAddress local, final IpAddress remote, Long vni, String vppNode)
    {
        final VxlanGpeBuilder vxlanGpeBuilder = new VxlanGpeBuilder();

        vxlanGpeBuilder.setLocal(local);
        vxlanGpeBuilder.setRemote(remote);
        vxlanGpeBuilder.setVni(new VxlanGpeVni(vni));
        vxlanGpeBuilder.setNextProtocol(VxlanGpeNextProtocol.Nsh);
        vxlanGpeBuilder.setEncapVrfId(Long.valueOf(0));
        vxlanGpeBuilder.setDecapVrfId(Long.valueOf(0));

        final RoutingBuilder routingBuilder = new RoutingBuilder();
        routingBuilder.setVrfId(Long.valueOf(0));

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
        String bridgeDomainName = SFC_BD_NAME;
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

        LOG.debug("Submitting new interface to config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Writing vxlangpe virtual interface to {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Writing vxlangpe virtual interface to {} failed.", vppNode, t);
            }
        });
    }

    private void removeVxlanGpePort(final DataBroker dataBroker, final IpAddress local, final IpAddress remote, Long vni, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(buildVxlanGpePortKey(remote)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIid);

        LOG.debug("Removing vxlangpe interface from config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Removing vxlangpe virtual interface from {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Removing vxlangpe virtual interface from {} failed.", vppNode, t);
            }
        });
    }

    private String buildVxlanPortKey(final IpAddress dstIp) {
        return new String("vxlanTun" + "_" + dstIp.getIpv4Address().getValue());
    }

    private void addVxlanPort(final DataBroker dataBroker, final IpAddress srcIp, final IpAddress dstIp, Long vni, String vppNode)
    {
        final VxlanBuilder vxlanBuilder = new VxlanBuilder();

        vxlanBuilder.setSrc(srcIp);
        vxlanBuilder.setDst(dstIp);
        vxlanBuilder.setVni(new VxlanVni(vni));
        vxlanBuilder.setEncapVrfId(Long.valueOf(0));

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
        String bridgeDomainName = SFC_BD_NAME;
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

        LOG.debug("Submitting new interface to config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Writing vxlan virtual interface to {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Writing vxlan virtual interface to {} failed.", vppNode, t);
            }
        });
    }

    private void removeVxlanPort(final DataBroker dataBroker, final IpAddress srcIp, final IpAddress dstIp, Long vni, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final KeyedInstanceIdentifier<Interface, InterfaceKey> interfaceIid
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(buildVxlanPortKey(dstIp)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, interfaceIid);

        LOG.debug("Removing vxlan interface from config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Removing vxlan virtual interface from {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Removing vxlan virtual interface from {} failed.", vppNode, t);
            }
        });
    }

    private String buildNshEntryKey(final Long nsp, final Short nsi) {
        return new String("nsh_entry_" + nsp.toString() + "_" + nsi.toString());
    }

    private void addNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode)
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
        Long c1 = Long.valueOf(0);
        Long c2 = Long.valueOf(0);
        Long c3 = Long.valueOf(0);
        Long c4 = Long.valueOf(0);
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

        LOG.debug("Submitting new nsh entry to config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Writing nsh entry to {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Writing nsh entry to {} failed.", vppNode, t);
            }
        });
    }

    private void removeNshEntry(final DataBroker dataBroker, final Long nsp, final Short nsi, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshEntry> nshEntryIid
                    = InstanceIdentifier.create(NshEntries.class).child(NshEntry.class, new NshEntryKey(buildNshEntryKey(nsp, nsi)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nshEntryIid);

        LOG.debug("Removing nsh entry from config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Removing nsh entry from {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Removing nsh entry from {} failed.", vppNode, t);
            }
        });
    }

    private String buildNshMapKey(final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi) {
        return new String("nsh_map_" + nsp.toString() + "_" + nsi.toString() + "_to_" + mappedNsp.toString() + "_" + mappedNsi.toString());
    }

    private void addNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi, String encapIfName, String vppNode)
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

        LOG.debug("Submitting new nsh map to config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Writing nsh map to {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Writing nsh map to {} failed.", vppNode, t);
            }
        });
    }

    private void removeNshMap(final DataBroker dataBroker, final Long nsp, final Short nsi, final Long mappedNsp, final Short mappedNsi, String vppNode) {
        final DataBroker vppDataBroker = dataBroker;
        final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<NshMap> nshMapIid
                    = InstanceIdentifier.create(NshMaps.class).child(NshMap.class, new NshMapKey(buildNshMapKey(nsp, nsi, mappedNsp, mappedNsi)));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nshMapIid);

        LOG.debug("Removing nsh map from config store...");

        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Removing nsh map from {} finished successfully.", vppNode);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Removing nsh map from {} failed.", vppNode, t);
            }
        });
    }

    private boolean configureVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp, final Short nsi) {
        Long vni = Long.valueOf(0); // SFC classifier set it to 0, so always use 0

        addVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue()); //SFF<->SF
        addNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To Next Hop
        addNshMap(dataBroker, nsp, nsi, nsp, nsi, buildVxlanGpePortKey(remoteIp), sffName.getValue());

        return true;
    }

    private boolean removeVxlanGpeNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress localIp, final IpAddress remoteIp, final Long nsp, final Short nsi) {
        Short nextNsi = nsi;
        nextNsi--;
        Long vni = Long.valueOf(0); // SFC classifier set it to 0, so always use 0
        removeVxlanGpePort(dataBroker, localIp, remoteIp, vni, sffName.getValue()); //SFF<->SF
        removeNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To SF
        removeNshEntry(dataBroker, nsp, nextNsi, sffName.getValue()); //From SF
        removeNshMap(dataBroker, nsp, nsi, nsp, nextNsi, sffName.getValue());
        return true;
    }

    private boolean configureVxlanNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress srcIp, final IpAddress dstIp, final Long nsp, final Short nsi) {
        Long vni = Long.valueOf(0); // SFC classifier set it to 0, so always use 0

        addVxlanPort(dataBroker, srcIp, dstIp, vni, sffName.getValue()); //SFF<->SF
        addNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To Next Hop
        addNshMap(dataBroker, nsp, nsi, nsp, nsi, buildVxlanPortKey(dstIp), sffName.getValue());

        return true;
    }

    private boolean removeVxlanNsh(final DataBroker dataBroker, final SffName sffName, final IpAddress srcIp, final IpAddress dstIp, final Long nsp, final Short nsi) {
        Short nextNsi = nsi;
        nextNsi--;
        Long vni = Long.valueOf(0); // SFC classifier set it to 0, so always use 0
        removeVxlanPort(dataBroker, srcIp, dstIp, vni, sffName.getValue()); //SFF<->SF
        removeNshEntry(dataBroker, nsp, nsi, sffName.getValue()); //To SF
        removeNshEntry(dataBroker, nsp, nextNsi, sffName.getValue()); //From SF
        removeNshMap(dataBroker, nsp, nsi, nsp, nextNsi, sffName.getValue());
        return true;
    }

    public void unregisterRspListener() {
        rspListener.getRegistrationObject().close();
    }
}
