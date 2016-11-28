/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.logicalclassifier;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapper;
import org.opendaylight.sfc.genius.util.appcoexistence.SfcTableIndexMapperBuilder;
import org.opendaylight.sfc.genius.util.servicebinding.GeniusServiceBinder;

public class ClassifierGeniusIntegration {
    private static final short TABLE_INDEX_CLASSIFIER = 0;

    private static final short TABLE_INDEX_INGRESS_TRANSPORT = 1;

    private static final SfcTableIndexMapper tableIndexMapper =
            new SfcTableIndexMapperBuilder()
                    .setClassifierTable(TABLE_INDEX_CLASSIFIER)
                    .setTransportIngressTable(TABLE_INDEX_INGRESS_TRANSPORT)
                    .build();

    // hide the default constructor
    private ClassifierGeniusIntegration() {}

    /**
     * Bind a logical interface, which we want to classify, to genius
     * @param theTx     a transaction object, in which the service binding will be attempted
     * @param ifName    the name of the logical interface
     */
    public static void performGeniusServiceBinding(WriteTransaction theTx, String ifName) {
        GeniusServiceBinder geniusBinder = new GeniusServiceBinder();
        geniusBinder.bindService(
                theTx,
                ifName,
                NwConstants.SFC_SERVICE_INDEX,
                getClassifierTable(),
                GeniusServiceBinder.getSfcIngressCookie(),
                GeniusServiceBinder.getSfcServicePriority(),
                NwConstants.SFC_SERVICE_NAME);
    }

    /**
     * Unbind a logical interface from genius
     * @param theTx     a transaction object, in which the service binding will be attempted
     * @param ifName    the name of the logical interface
     */
    public static void performGeniusServiceUnbinding(WriteTransaction theTx, String ifName) {
        GeniusServiceBinder geniusBinder = new GeniusServiceBinder();
        geniusBinder.unbindService(
                theTx,
                ifName,
                NwConstants.SFC_SERVICE_INDEX);
    }

    /**
     * Get the number of the openflow table used by the SFC classifier from genius
     * @return  the number of the openflow table used by the SFC classifier
     */
    public static short getClassifierTable() {
        // get the genius offset table, or go w/ the default classifier table (0)
        return tableIndexMapper.getTableIndex(TABLE_INDEX_CLASSIFIER).isPresent() ?
                tableIndexMapper.getTableIndex(TABLE_INDEX_CLASSIFIER).get() : TABLE_INDEX_CLASSIFIER;
    }

    /**
     * Get the number of the openflow table used by SFC transport ingress from genius
     * @return  the number of the openflow table used by the SFC transport ingress table
     */
    public static short getTransportIngressTable() {
        // get the genius offset table, or go w/ the default classifier table (0)
        return tableIndexMapper.getTableIndex(TABLE_INDEX_INGRESS_TRANSPORT).isPresent() ?
                tableIndexMapper.getTableIndex(TABLE_INDEX_INGRESS_TRANSPORT).get() : TABLE_INDEX_INGRESS_TRANSPORT;
    }
}
