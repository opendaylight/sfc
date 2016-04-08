package org.opendaylight.sfc.sfc_netconf.provider;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfNodeManager;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfRspManager;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfSfManager;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfSffManager;

public class SfcNetconfRenderer {

    private SfcNetconfNodeManager nodeManager;
    private SfcNetconfSffManager forwarderManager;
    private SfcNetconfSfManager functionManager;
    private SfcNetconfRspManager renderedPathManager;

    public SfcNetconfRenderer(DataBroker dataBroker, BindingAwareBroker bindingAwareBroker) {
        nodeManager = new SfcNetconfNodeManager(dataBroker, bindingAwareBroker, this);
        forwarderManager = new SfcNetconfSffManager(dataBroker, this);
        functionManager = new SfcNetconfSfManager(dataBroker, this);
        renderedPathManager = new SfcNetconfRspManager(dataBroker, this);
    }

    public void unregisterListeners() {
        nodeManager.unregisterNodeListener();
        forwarderManager.unregisterSffListener();
        functionManager.unregisterSfListener();
        renderedPathManager.unregisterRspListener();
    }

    public SfcNetconfNodeManager getNodeManager() {
        return nodeManager;
    }

    public SfcNetconfSffManager getForwarderManager() {
        return forwarderManager;
    }

    public SfcNetconfSfManager getFunctionManager() {
        return functionManager;
    }

    public SfcNetconfRspManager getRenderedPathManager() {
        return renderedPathManager;
    }


}
