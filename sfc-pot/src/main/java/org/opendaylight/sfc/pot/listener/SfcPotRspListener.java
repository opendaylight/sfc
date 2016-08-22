/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.listener;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.sfc.pot.provider.SfcPotRspProcessor;

import java.util.Map;

/* RSP delete event registration and handler */
public class SfcPotRspListener extends SfcPotAbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotRspListener.class);

    private final SfcPotRspProcessor sfcPotRspProcessor;

    public SfcPotRspListener(OpendaylightSfc opendaylightSfc,
                             SfcPotRspProcessor sfcPotRspProcessor) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.RSP_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.OPERATIONAL);
        this.sfcPotRspProcessor = sfcPotRspProcessor;
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        //NOTE: only interested in RSP deletion for cleanup
        for (InstanceIdentifier iid : change.getRemovedPaths()) {
            if (dataOriginalDataObject.get(iid) instanceof RenderedServicePath) {
                RenderedServicePath rsp = (RenderedServicePath) dataOriginalDataObject.get(iid);
                LOG.debug("Deleted RenderedServicePath: {}", rsp.getName());
                sfcPotRspProcessor.deleteRspEvent(rsp);
            }
        }
    }
}
