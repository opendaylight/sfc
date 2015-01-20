package org.opendaylight.sfc.sbrest.provider.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSfTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SbRestSfEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfEntryDataListener.class);

    public SbRestSfEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SF_ENTRY_IID);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        System.out.println("\n***SB REST sf listener***\n");
        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                System.out.println("*** sb-Original Service function: " +
                        originalServiceFunction.getName());
                LOG.debug("\n########## Original Service function: {}",
                        originalServiceFunction.getName());
            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();
                LOG.error("*** created Service Function: {}", createdServiceFunction.getName());
                System.out.println("*** sb-created Service Function: " + createdServiceFunction.getName());

                Object result = null;
                try {
                    result = opendaylightSfc.getExecutor().submit(SfcProviderServiceForwarderAPI
                            .getReadAll(new Object[]{}, new Class[]{})).get();
                    ServiceFunctionForwarders serviceFunctionForwarders = (ServiceFunctionForwarders) result;

                    for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarders.getServiceFunctionForwarder()) {
                        Uri uri = serviceFunctionForwarder.getRestUri();
                        //String urlMgmt = uri.getValue();
                        String urlMgmt = "127.0.0.100";
                        LOG.info("PUT url:{}", urlMgmt);
                        //SbRestPutSfTask putSfTask = new SbRestPutSfTask(createdServiceFunction, urlMgmt); // Deprecated
                        SbRestSfTask task = new SbRestSfTask(RestOperation.POST, createdServiceFunction, urlMgmt);
                        opendaylightSfc.getExecutor().submit(task);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

/*        // SFP UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject =
                change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionPath) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
                ServiceFunctionPath updatedServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Modified Service Function Path Name {}",
                        updatedServiceFunctionPath.getName());

                if (SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(updatedServiceFunctionPath.getName()) != null) {
                    renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(updatedServiceFunctionPath);
                    if (renderedServicePath != null) {
                        SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                (updatedServiceFunctionPath.getClassifier(), renderedServicePath.getName());

                        if (updatedServiceFunctionPath.isSymmetric() != null && updatedServiceFunctionPath.isSymmetric() &&
                                (SfcProviderServiceClassifierAPI
                                        .readServiceClassifierExecutor(updatedServiceFunctionPath.getSymmetricClassifier()) != null)) {

                            revRenderedServicePath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(renderedServicePath);
                            if (revRenderedServicePath != null) {
                                SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor
                                        (updatedServiceFunctionPath.getSymmetricClassifier(), revRenderedServicePath.getName());

                            }
                        }
                    }
                }
            }
        }*/


        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunction) {

                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                LOG.error("XXXXXXX Service Function Name is {}", originalServiceFunction.getName());

            }
        }
        printTraceStop(LOG);
    }


}
