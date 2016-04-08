package org.opendaylight.sfc.sfc_netconf.provider.renderer;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.sfc_netconf.provider.SfcNetconfRenderer;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceFunctionAPI;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceFunctionForwarderAPI;
import org.opendaylight.sfc.sfc_netconf.provider.listener.SfcNetconfRspDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.Services;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.ServiceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.Terminate;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.TerminateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.READ_FUNCTION;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.READ_LOCAL;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.READ_REMOTE;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.UPDATE_REMOTE;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.WRITE_PATH;

public class SfcNetconfRspManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfRspManager.class);

    private SfcNetconfRenderer renderer;
    private SfcNetconfRspDataChangeListener rspListener;

    public SfcNetconfRspManager(DataBroker dataBroker, SfcNetconfRenderer renderer) {
        this.renderer = renderer;
        // Register RSP listener
        rspListener = new SfcNetconfRspDataChangeListener(dataBroker, this);
    }

    public void syncRsp(List<RenderedServicePath> renderedServicePaths) {
        for(RenderedServicePath renderedServicePath : renderedServicePaths) {
            Long pathId = renderedServicePath.getPathId();
            Short serviceIndex = renderedServicePath.getStartingIndex();

            DataBroker previousMountPoint;  // DataBroker to access previous hop node/local SFF
            DataBroker currentMountpoint;   // DataBroker to access current hop/local SFF
            SffName previousSffName;        // Name of previous forwarder
            SffName currentSffName;         // Name of current forwarder
            Iterator<RenderedServicePathHop> rspHopIterator = renderedServicePath.getRenderedServicePathHop()
                    .iterator();
            // Proceed first hop in Rsp. Service Type choice for first hop is always Service Function
            RenderedServicePathHop hop = rspHopIterator.next();
            currentSffName = hop.getServiceFunctionForwarder();
            currentMountpoint = getSffMountpoint(currentSffName);
            if (currentMountpoint == null) {
                LOG.error("Resolving of RSP {} failed, mountpoint for SFF {} is null", renderedServicePath.getName()
                .getValue(), currentSffName.getValue());
                return;
            }
            // New list of services has to be created every time new mountpoint is created
            List<Services> services = new ArrayList<>();
            SfName sfName = hop.getServiceFunctionName();
            ServiceTypeChoice serviceTypeChoice = (ServiceTypeChoice) new SfcNetconfDataStoreAPI(currentMountpoint,
                    sfName, READ_FUNCTION).call();
            Services serviceEntry = createServicesEntry(hop.getHopNumber(), serviceIndex,
                    serviceTypeChoice);
            services.add(serviceEntry);
            serviceIndex--;

            while(rspHopIterator.hasNext()) {
                hop = rspHopIterator.next();
                // Find out whether next hop SF is connected to the same SFF
                previousSffName = currentSffName;
                currentSffName = hop.getServiceFunctionForwarder();
                if (previousSffName.equals(currentSffName)) {
                    // Next hop SF is on the same local SFF/node as the previous one
                    sfName = hop.getServiceFunctionName();
                    serviceTypeChoice = (ServiceTypeChoice) new SfcNetconfDataStoreAPI(currentMountpoint,
                            sfName, READ_FUNCTION).call();
                    serviceEntry = createServicesEntry(hop.getHopNumber(), serviceIndex,
                            serviceTypeChoice);
                    services.add(serviceEntry);
                    serviceIndex--;
                }
                else {
                    // Next hop SF is on different node. Store previous SFF and its mountpoint
                    previousMountPoint = currentMountpoint;
                    currentSffName = hop.getServiceFunctionForwarder();
                    currentMountpoint = getSffMountpoint(currentSffName);
                    if (currentMountpoint == null) {
                        LOG.error("Resolving of RSP {} failed, mountpoint for SFF {} is null", renderedServicePath.getName()
                                .getValue(), currentSffName.getValue());
                        return;
                    }
                    // Write current SFF to previous SFF node as remote
                    ServiceFunctionForwarder currentRemoteForwarder = SfcNetconfServiceFunctionForwarderAPI
                            .convertLocalToRemote(currentSffName);
                    new SfcNetconfDataStoreAPI(previousMountPoint, currentRemoteForwarder, UPDATE_REMOTE).call();
                    // Write previous SFF to current SFF node as remote
                    ServiceFunctionForwarder previousRemoteForwarder = SfcNetconfServiceFunctionForwarderAPI
                            .convertLocalToRemote(previousSffName);
                    new SfcNetconfDataStoreAPI(currentMountpoint, previousRemoteForwarder, UPDATE_REMOTE).call();
                    // Create last service entry to previous node which sends traffic to current node
                    serviceTypeChoice = (ServiceTypeChoice) new SfcNetconfDataStoreAPI(currentMountpoint,
                            currentSffName, READ_REMOTE).call();
                    serviceEntry = createServicesEntry(hop.getHopNumber(), serviceIndex,
                            serviceTypeChoice);
                    services.add(serviceEntry);
                    // List of services completed for last mountpoint, create service path entries and write it
                    ServicePath servicePath = createServicePath(pathId, services);
                    new SfcNetconfDataStoreAPI(previousMountPoint, servicePath, WRITE_PATH);
                    // Start with new services list
                    services = new ArrayList<>();
                    sfName = hop.getServiceFunctionName();
                    serviceTypeChoice = (ServiceTypeChoice) new SfcNetconfDataStoreAPI(currentMountpoint,
                            sfName, READ_FUNCTION).call();
                    serviceEntry = createServicesEntry(hop.getHopNumber(), serviceIndex,
                            serviceTypeChoice);
                    services.add(serviceEntry);
                    serviceIndex--;
                }
            }

            // Proceed last entry (it's the same hop as the previous one using same mountpoint and list of services)
            // Service Type choice is always Terminate
            TerminateBuilder terminateBuilder = new TerminateBuilder();
            terminateBuilder.setTerminate(true);
            serviceTypeChoice = terminateBuilder.build();
            serviceEntry = createServicesEntry(hop.getHopNumber(), serviceIndex, serviceTypeChoice);
            services.add(serviceEntry);
            // List of services completed for last mountpoint, create last service path entries and write it
            ServicePath servicePath = createServicePath(pathId, services);
            new SfcNetconfDataStoreAPI(currentMountpoint, servicePath, WRITE_PATH);
        }
    }

    private ServicePath createServicePath(Long pathId, List<Services> services) {
        // Service Index
        ServiceIndexBuilder serviceIndexBuilder = new ServiceIndexBuilder();
        serviceIndexBuilder.setServices(services);
        // Service Chain Path Mode
        ConfigServiceChainPathModeBuilder pathModeBuilder = new ConfigServiceChainPathModeBuilder();
        pathModeBuilder.setServiceIndex(serviceIndexBuilder.build());
        // Service Path
        ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
        servicePathBuilder.setKey(new ServicePathKey(pathId))
                .setServicePathId(pathId)
                .setConfigServiceChainPathMode(pathModeBuilder.build());
        return servicePathBuilder.build();
    }

    private Services createServicesEntry(Short key, short index, ServiceTypeChoice choice) {
        ServicesBuilder servicesBuilder = new ServicesBuilder();
        servicesBuilder.setKey(new ServicesKey(key))
                .setServiceIndexId(index)
                .setServiceTypeChoice(choice);
        return servicesBuilder.build();
    }

    private DataBroker getSfMountpoint(SfName sfName) {
        // Read SF from Controller CONF
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction serviceFunction =
                SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
        IpAddress mgmtIp = serviceFunction.getIpMgmtAddress();

        return renderer.getNodeManager().getMountpointFromIp(new IpAddress(new Ipv4Address(mgmtIp.getIpv4Address()
                .getValue())));
    }

    private DataBroker getSffMountpoint(SffName sffName) {
        // Read SFF from Controller CONF
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder sfcForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        IpAddress sffMgmtIp = sfcForwarder.getIpMgmtAddress();
        if (sffMgmtIp == null) {
            LOG.error("Unable to obtain management IP for SFF {}", sffName.getValue());
            return null;
        }
        return renderer.getNodeManager().getMountpointFromIp(new IpAddress(new Ipv4Address(sffMgmtIp.getIpv4Address()
                .getValue())));
    }

    // TODO this method is in every manager
    private IpAddress getNetconfNodeIp(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        return netconfNode.getHost().getIpAddress();
    }

    public void unregisterRspListener() {
        rspListener.getRegistrationObject().close();
    }
}
