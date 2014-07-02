/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds all RPCs methods for SFC Provider.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderSffDataListener implements DataChangeListener  {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSnDataListener.class);

    @Override
    public void onDataChanged(
            DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);


        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

}
