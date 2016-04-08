package org.opendaylight.sfc.sfc_netconf.provider.renderer;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.sfc_netconf.provider.SfcNetconfRenderer;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceFunctionForwarderAPI;
import org.opendaylight.sfc.sfc_netconf.provider.listener.SfcNetconfSffDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.DELETE_LOCAL;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.DELETE_REMOTE;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.UPDATE_LOCAL;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.UPDATE_REMOTE;

public class SfcNetconfSffManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfSffManager.class);

    private SfcNetconfRenderer renderer;
    private SfcNetconfSffDataChangeListener sffListener;

    public SfcNetconfSffManager(DataBroker dataBroker, SfcNetconfRenderer renderer) {
        this.renderer = renderer;
        // Register SFF listener
        sffListener = new SfcNetconfSffDataChangeListener(dataBroker, this);
    }

    public void syncForwarders(List<ServiceFunctionForwarder> forwarders, boolean delete) {
        for(ServiceFunctionForwarder forwarder : forwarders) {
            LOG.info("Forwarder to resolve: {}", forwarder.getName().getValue());   //REMOVE
            IpAddress forwarderMgmtIp = forwarder.getIpMgmtAddress();
            if (forwarderMgmtIp == null) {
                LOG.warn("Service function forwarder {} has no management Ip address, cannot be created",
                        forwarder.getName().getValue());
                continue;
            }
            // Find appropriate node for SFF
            for (Node netconfNode : renderer.getNodeManager().getConnectedNodes().values()) {
                IpAddress netconfNodeIp = getNetconfNodeIp(netconfNode);
                if (netconfNodeIp.equals(forwarderMgmtIp)) {
                    // Find the right mountpoint
                    DataBroker mountPoint = renderer.getNodeManager().getActiveMountPoints()
                            .get(netconfNode.getNodeId());
                    LOG.info("Mountpoint is {} ", mountPoint);  //REMOVE
                    if (mountPoint != null) {
                        for (SffDataPlaneLocator forwarderDpl : forwarder.getSffDataPlaneLocator()) {
                            DataPlaneLocator dpl = forwarderDpl.getDataPlaneLocator();
                            LocatorType locatorType = dpl.getLocatorType();
                            Ip sffIp = null;
                            if (locatorType instanceof Ip) {
                                LOG.debug("IP locator found: {} ", locatorType);
                                sffIp = (Ip) locatorType;
                            }
                            if (sffIp != null && sffIp.getIp() != null) {
                                IpAddress ipAddress = sffIp.getIp();
                                // Create/remove local SFF
                                org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder localForwarder =
                                        SfcNetconfServiceFunctionForwarderAPI.createLocalForwarder(forwarder, ipAddress);
                                if (localForwarder != null && !delete) {
                                    LOG.info("Writing local forwarder ... {} ...", localForwarder); //REMOVE
                                    new SfcNetconfDataStoreAPI(mountPoint, localForwarder.getLocal(), UPDATE_LOCAL)
                                            .call();
                                }
                                if (localForwarder != null && delete) {
                                    LOG.info("Deleting local forwarder ... {} ...", localForwarder);    //REMOVE
                                    new SfcNetconfDataStoreAPI(mountPoint, localForwarder.getLocal(), DELETE_LOCAL)
                                            .call();

                                }
                            }
                        }
                    }
                }
                else {
                    LOG.warn("No node found for SFF {}", forwarder.getName());
                }
            }
        }
    }

    private IpAddress getNetconfNodeIp(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        return netconfNode.getHost().getIpAddress();
    }

    public void unregisterSffListener() {
        sffListener.getRegistrationObject().close();
    }

}
