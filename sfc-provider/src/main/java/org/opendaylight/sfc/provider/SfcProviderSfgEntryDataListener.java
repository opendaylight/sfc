/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;


import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev160202.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class gets called whenever there is a change to
 * a Service Function Group list entry, i.e.,
 * added/deleted/modified.
 *
 * <p
 * @author Shlomi Alfasi (shlomi.alfasi@contextream.com)
 * @version 0.1
 * @since       2015-03-08
 */
public class SfcProviderSfgEntryDataListener implements DataChangeListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfgEntryDataListener.class);
    private final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();


    /**
     * This method is called whenever there is change in a SF. Before doing any changes
     * it takes a global lock in order to ensure it is the only writer.
     * @param change
     */
    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        printTraceStart(LOG);

        odlSfc.getLock();

        // SF ORIGINAL
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunctionGroup) {
            }
        }

        // SFG CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof ServiceFunctionGroup) {
            }
        }

        // SFG DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier<?> instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if( dataObject instanceof ServiceFunctionGroup) {
            }
        }


        // SFG UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject
                = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionGroup) && (!(dataCreatedObject.containsKey(entry.getKey())))) {
            }
        }
        odlSfc.releaseLock();
        printTraceStop(LOG);
    }

}
