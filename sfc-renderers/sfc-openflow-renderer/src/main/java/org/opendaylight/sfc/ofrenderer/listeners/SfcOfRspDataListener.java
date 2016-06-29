/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.listeners;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfBaseProviderUtils;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class has will be notified when changes are mad to Rendered Service Paths.
 *
 * @author Brady Johnson (brady.allen.johnson@ericsson.com)
 * @version 0.1
 * <p>
 * @since 2015-01-27
 */
public class SfcOfRspDataListener extends SfcOfAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRspDataListener.class);
    private SfcOfRspProcessor sfcOfRspProcessor;

    public SfcOfRspDataListener(
            DataBroker dataBroker,
            SfcOfFlowProgrammerInterface sfcOfFlowProgrammer,
            SfcOfBaseProviderUtils sfcOfProviderUtils,
            SfcSynchronizer sfcSynchronizer) {
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.RSP_ENTRY_IID);
        registerAsDataChangeListener(LogicalDatastoreType.OPERATIONAL);
        this.sfcOfRspProcessor = new SfcOfRspProcessor(sfcOfFlowProgrammer, sfcOfProviderUtils, sfcSynchronizer);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        // Currently we dont need to do anything for the OriginalData

        // configureSffFlows will do a check for each SFF to see
        // if its Openflow Enabled, and if not, skip it

        // RSP create
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                LOG.info("SfcOfRspDataListener.onDataChanged create RSP {}", ((RenderedServicePath) entry.getValue()).getName());
                this.sfcOfRspProcessor.processRenderedServicePath((RenderedServicePath) entry.getValue());
            }
        }

        // RSP update
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof RenderedServicePath && (!(dataCreatedConfigurationObject.containsKey(entry.getKey()))))) {
                LOG.info("SfcOfRspDataListener.onDataChanged update RSP {}", ((RenderedServicePath) entry.getValue()).getName());
                // Currently RSP updates are not supported
            }
        }

        // RSP delete
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier<?> instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = change.getOriginalData().get(instanceIdentifier);
            if (dataObject instanceof RenderedServicePath) {
                LOG.info("SfcOfRspDataListener.onDataChanged delete RSP");
                this.sfcOfRspProcessor.deleteRenderedServicePath((RenderedServicePath) dataObject);
            }
        }
    }
}
