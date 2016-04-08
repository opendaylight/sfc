package org.opendaylight.sfc.sfc_netconf.provider.listener;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfSffManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

public class SfcNetconfSffDataChangeListener implements DataTreeChangeListener<ServiceFunctionForwarders> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcNetconfSffDataChangeListener.class);

    private ListenerRegistration netconfSffListenerRegistration;
    private SfcNetconfSffManager sffManager;

    public SfcNetconfSffDataChangeListener(DataBroker dataBroker, SfcNetconfSffManager sffManager) {
        this.sffManager = sffManager;
        // Register listener
        netconfSffListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(ServiceFunctionForwarders.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServiceFunctionForwarders>> changes) {
        for(DataTreeModification<ServiceFunctionForwarders> modification : changes) {
            DataObjectModification<ServiceFunctionForwarders> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if(rootNode.getDataAfter() != null
                            && rootNode.getDataAfter().getServiceFunctionForwarder() != null) {
                        sffManager.syncForwarders(rootNode.getDataAfter().getServiceFunctionForwarder(), false);
                    }
                    break;
                case DELETE:
                    if(rootNode.getDataBefore() != null
                            && rootNode.getDataBefore().getServiceFunctionForwarder() != null) {
                        sffManager.syncForwarders(rootNode.getDataBefore().getServiceFunctionForwarder(), true);
                    }
                    break;
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return netconfSffListenerRegistration;
    }
}
