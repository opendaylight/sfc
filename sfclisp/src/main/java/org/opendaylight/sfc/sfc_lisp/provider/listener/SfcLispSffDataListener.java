/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_lisp.provider.listener;

import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.sfc_lisp.provider.LispUpdater;
import org.opendaylight.sfc.sfc_lisp.provider.api.SfcProviderServiceLispAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is the DataListener for SFF changes.
 *
 * <p>
 * @author David Goldberg (david.goldberg@contextream.com)
 * @author Florin Coras (fcoras@cisco.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcLispSffDataListener extends SfcLispAbstractDataListener  {

    private static final Logger LOG = LoggerFactory.getLogger(SfcLispSffDataListener.class);
    private LispUpdater lispUpdater;

    public SfcLispSffDataListener(OpendaylightSfc odlSfc, LispUpdater lispUpdater) {
        setOpendaylightSfc(odlSfc);
        setDataBroker(odlSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SFF_IID);
        setDataStoreType(LogicalDatastoreType.CONFIGURATION);
        registerAsDataChangeListener();
        this.lispUpdater = lispUpdater;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet())
        {
            if( entry.getValue() instanceof ServiceFunctionForwarders) {

                ServiceFunctionForwarders updatedServiceFunctionForwarders = (ServiceFunctionForwarders) entry.getValue();
                List<ServiceFunctionForwarder> serviceFunctionForwarderList = updatedServiceFunctionForwarders.getServiceFunctionForwarder();
                for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
                    if (lispUpdater.containsLispAddress(serviceFunctionForwarder)) {
                        SfcProviderServiceLispAPI.lispUpdateServiceFunctionForwarder(serviceFunctionForwarder);
                    }
                }
            }
        }


        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
