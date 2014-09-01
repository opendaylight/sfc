/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.ofsfc.provider;


import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gets called whenever there is a change to
 * the Service Functions data store.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */

public class OfSfcProviderSfsDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(OfSfcProviderSfsDataListener.class);
    private OfSfcProvider odlSfc = OfSfcProvider.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);



        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
 }
