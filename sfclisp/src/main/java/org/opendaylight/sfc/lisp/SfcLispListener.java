package org.opendaylight.sfc.lisp;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class SfcLispListener implements ISfcLispListener, BindingAwareConsumer {

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        DataBroker dataBrokerService = session.getSALService(DataBroker.class);

        // ServiceFunctionFowarder Entry
        SfcLispProviderSffEntryDataListener sfcProviderSffDataListener = new SfcLispProviderSffEntryDataListener();
        dataBrokerService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.sffIID, sfcProviderSffDataListener,
                DataBroker.DataChangeScope.SUBTREE);

        // ServiceFunction Entry
        SfcLispProviderSfEntryDataListener sfcProviderSfEntryDataListener = new SfcLispProviderSfEntryDataListener();
        dataBrokerService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.sfEntryIID, sfcProviderSfEntryDataListener,
                DataBroker.DataChangeScope.SUBTREE);

    }

    void setBindingAwareBroker(BindingAwareBroker bindingAwareBroker) {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bindingAwareBroker.registerConsumer(this, bundleContext);
    }

}
