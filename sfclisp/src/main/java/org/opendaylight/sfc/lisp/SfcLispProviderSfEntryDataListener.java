/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.lisp;


import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
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
 * <p
 * @author David Goldberg (david.goldberg@contextream.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcLispProviderSfEntryDataListener implements DataChangeListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcLispProviderSfEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static LispUpdater lispUpdater = LispUpdater.getLispUpdaterObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();

                Object[] serviceTypeObj = { createdServiceFunction };
                Class[] serviceTypeClass = { ServiceFunction.class };

                if (lispUpdater.containsLispAddress(createdServiceFunction)) {
                    odlSfc.executor.submit(SfcProviderServiceLispAPI.getUpdateServiceFunction(serviceTypeObj, serviceTypeClass));
                }

            }

        }

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
