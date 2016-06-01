/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationIoamAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class SfcVerifyRSPDataListener extends SfcVerifyAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyRSPDataListener.class);

    private final SfcVerificationIoamAPI sfcVerificationIoamAPI;

    public SfcVerifyRSPDataListener(OpendaylightSfc opendaylightSfc,
                                    SfcVerificationIoamAPI sfcVerificationIoamAPI) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.RSP_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        this.sfcVerificationIoamAPI = sfcVerificationIoamAPI;
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        // RSP creation
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath) {
                RenderedServicePath rsp = (RenderedServicePath) entry.getValue();
                LOG.debug("Created RenderedServicePath: {}", rsp.getName());
                //As of now, it is not expected that SFCV configurations
                //will be configured as part of the RSP creation itself.
            }
        }

        // RSP update
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof RenderedServicePath &&
                    !dataCreatedObject.containsKey(entry.getKey())) {
                RenderedServicePath rsp = (RenderedServicePath) entry.getValue();
                LOG.debug("Updated RenderedServicePath: {}", rsp.getName());
                sfcVerificationIoamAPI.processRspUpdate(rsp);
            }
        }

        //RSP deletion
        for (InstanceIdentifier iid : change.getRemovedPaths()) {
            if (dataOriginalDataObject.get(iid) instanceof RenderedServicePath) {
                RenderedServicePath rsp = (RenderedServicePath) dataOriginalDataObject.get(iid);
                LOG.debug("Deleted RenderedServicePath: {}", rsp.getName());
                sfcVerificationIoamAPI.deleteRsp(rsp);
            }
        }
    }
}
