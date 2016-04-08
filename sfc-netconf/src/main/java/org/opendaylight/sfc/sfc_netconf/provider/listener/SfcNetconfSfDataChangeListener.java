package org.opendaylight.sfc.sfc_netconf.provider.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.sfc_netconf.provider.renderer.SfcNetconfSfManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.Collection;

public class SfcNetconfSfDataChangeListener implements DataTreeChangeListener<ServiceFunctions> {

    private SfcNetconfSfManager sfManager;
    private ListenerRegistration netconfSfListenerRegistration;

    public SfcNetconfSfDataChangeListener(DataBroker dataBroker, SfcNetconfSfManager sfManager) {
        this.sfManager = sfManager;
        // Register listener
        netconfSfListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(ServiceFunctions.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServiceFunctions>> changes) {
        for(DataTreeModification<ServiceFunctions> modification : changes) {
            DataObjectModification<ServiceFunctions> rootNode = modification.getRootNode();

            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if(rootNode.getDataAfter() != null && rootNode.getDataAfter().getServiceFunction() != null) {
                        sfManager.syncFunctions(rootNode.getDataAfter().getServiceFunction(), false);
                    }
                    break;
                case DELETE:
                    if(rootNode.getDataBefore() != null && rootNode.getDataBefore().getServiceFunction() != null) {
                        sfManager.syncFunctions(rootNode.getDataBefore().getServiceFunction(), true);
                    }
                    break;
            }
        }
    }

    public ListenerRegistration getRegistrationObject() {
        return netconfSfListenerRegistration;
    }

}
