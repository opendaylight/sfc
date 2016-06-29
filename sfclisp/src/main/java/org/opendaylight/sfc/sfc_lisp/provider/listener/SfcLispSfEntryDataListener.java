/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_lisp.provider.listener;


import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_lisp.provider.LispUpdater;
import org.opendaylight.sfc.sfc_lisp.provider.api.SfcProviderServiceLispAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gets called whenever there is a change to
 * a Service Function list entry, i.e.,
 * added.
 *
 * <p>
 * @author David Goldberg (david.goldberg@contextream.com)
 * @author Florin Coras (fcoras@cisco.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcLispSfEntryDataListener extends SfcLispAbstractDataListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcLispSfEntryDataListener.class);
    private LispUpdater lispUpdater;

    public SfcLispSfEntryDataListener(OpendaylightSfc odlSfc, LispUpdater lispUpdater) {
        setOpendaylightSfc(odlSfc);
        setDataBroker(odlSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SF_ENTRY_IID);
        setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        registerAsDataChangeListener();
        this.lispUpdater = lispUpdater;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();

                if (lispUpdater.containsLispAddress(createdServiceFunction)) {
                    SfcProviderServiceLispAPI.lispUpdateServiceFunction(createdServiceFunction);
                }

            }

        }

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
