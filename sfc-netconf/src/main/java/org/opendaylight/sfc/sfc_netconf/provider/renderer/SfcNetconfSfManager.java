package org.opendaylight.sfc.sfc_netconf.provider.renderer;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.sfc_netconf.provider.SfcNetconfRenderer;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI;
import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceFunctionAPI;
import org.opendaylight.sfc.sfc_netconf.provider.listener.SfcNetconfSfDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.util.List;

import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.DELETE_FUNCTION;
import static org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfDataStoreAPI.Transaction.UPDATE_FUNCTION;


public class SfcNetconfSfManager {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfSfManager.class);

    private SfcNetconfRenderer renderer;
    private SfcNetconfSfDataChangeListener sfListener;

    public SfcNetconfSfManager(DataBroker dataBroker, SfcNetconfRenderer renderer) {
        this.renderer = renderer;
        // Register SF listener
        sfListener = new SfcNetconfSfDataChangeListener(dataBroker, this);
    }

    public void syncFunctions(List<ServiceFunction> functions, boolean delete) {
        for (ServiceFunction function : functions) {
            LOG.info("Service function to resolve {}", function.getName().getValue()); //REMOVE
            IpAddress forwarderMgmtIp = function.getIpMgmtAddress();
            if (forwarderMgmtIp == null) {
                LOG.warn("Service function forwarder {} has no management Ip address, cannot be created",
                        function.getName().getValue());
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
                    if (mountPoint != null && !delete) {
                        org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction netconfServiceFunction =
                                SfcNetconfServiceFunctionAPI.createFunction(function);
                        LOG.info("Writing service function ... {} ...", netconfServiceFunction);
                        new SfcNetconfDataStoreAPI(mountPoint, netconfServiceFunction, UPDATE_FUNCTION).call();    //REMOVE
                    }
                    if (mountPoint != null && delete) {
                        org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction netconfServiceFunction =
                                SfcNetconfServiceFunctionAPI.createFunction(function);
                        LOG.info("Writing service function ... {} ...", netconfServiceFunction);
                        new SfcNetconfDataStoreAPI(mountPoint, netconfServiceFunction, DELETE_FUNCTION).call();    //REMOVE
                    }
                }
            }
        }
    }

    private IpAddress getNetconfNodeIp(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        Preconditions.checkNotNull(netconfNode);
        return netconfNode.getHost().getIpAddress();
    }


    public void unregisterSfListener() {
        sfListener.getRegistrationObject().close();
    }

}
