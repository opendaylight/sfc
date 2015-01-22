package org.opendaylight.sfc.sbrest.provider.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;
@Deprecated
public class SbRestSfpEntryDataListener extends SbRestAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SbRestSfpEntryDataListener.class);

    public SbRestSfpEntryDataListener(OpendaylightSfc opendaylightSfc) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SFP_ENTRY_IID);
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);
        opendaylightSfc.getLock();

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPath) {
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) entry.getValue();
                LOG.debug("\n########## Original Service path: {}",
                        originalServiceFunctionPath.getName());
            }
        }

        // SFP CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        /* For each SFP we perform the following transactions:
         *   0 - Check if classifier exists
         *   1 - Create RSP
         *   2 - Add Path to SFF State
         *   3 - Add path to SF state
         *   4 - Check if Chain is symmetric
         *   5 - if chain is symmetric repeat 1-3 for reverse path
         *
         * If any of these fail we delete the previous ones that succeeded.
         */

        RenderedServicePath renderedServicePath;
        RenderedServicePath revRenderedServicePath;
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionPath) {

                ServiceFunctionPath createdServiceFunctionPath = (ServiceFunctionPath) entry.getValue();


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


        // SFP DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionPath) {

                // If a SFP is deleted we remove RSP and both SF and SFF operational states.
                ServiceFunctionPath originalServiceFunctionPath = (ServiceFunctionPath) dataObject;
                SfcProviderServiceForwarderAPI
                        .deletePathFromServiceForwarderStateExecutor(originalServiceFunctionPath);
                SfcProviderServiceFunctionAPI
                        .deleteServicePathFromServiceFunctionStateExecutor(originalServiceFunctionPath);
                LOG.error("XXXXXXX Service Path Name is {}", originalServiceFunctionPath.getName());

                List<SfpRenderedServicePath> sfpRenderedServicePathList = SfcProviderServicePathAPI
                        .readServicePathStateExecutor(originalServiceFunctionPath.getName());
                if ((sfpRenderedServicePathList != null) && (!sfpRenderedServicePathList.isEmpty())) {
                    for (SfpRenderedServicePath sfpRenderedServicePath : sfpRenderedServicePathList) {
                        String rspName = sfpRenderedServicePath.getName();
                        LOG.error("XXXXXXX Rendered Path Name is {}", rspName);
                        SfcProviderRenderedPathAPI.deleteRenderedServicePathExecutor(rspName);
                        //Send to SB REST
                        //SfcProviderServicePathAPI.checkServiceFunctionPathExecutor
                        //        (originalServiceFunctionPath, HttpMethod.DELETE);
                    }
                }
                SfcProviderServicePathAPI.deleteServicePathStateExecutor(originalServiceFunctionPath.getName());
            }
        }
        opendaylightSfc.releaseLock();
        printTraceStop(LOG);
    }


}
