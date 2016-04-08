package org.opendaylight.sfc.sfc_netconf.provider.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfRspManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.Collection;

public class SfcNetconfRspDataChangeListener implements DataTreeChangeListener<RenderedServicePaths> {

    private SfcNetconfRspManager rspManager;
    private ListenerRegistration netconfRspListenerRegistration;

    public SfcNetconfRspDataChangeListener(DataBroker dataBroker, SfcNetconfRspManager rspManager) {
        this.rspManager = rspManager;
        // Register listener
        netconfRspListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(RenderedServicePaths.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<RenderedServicePaths>> changes) {
        for (DataTreeModification<RenderedServicePaths> modification : changes) {
            DataObjectModification<RenderedServicePaths> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if(rootNode.getDataAfter() != null && rootNode.getDataAfter().getRenderedServicePath() != null) {
                        rspManager.syncRsp(rootNode.getDataAfter().getRenderedServicePath());
                    }
                    break;
                case DELETE:
                    break;
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return netconfRspListenerRegistration;
    }
}
