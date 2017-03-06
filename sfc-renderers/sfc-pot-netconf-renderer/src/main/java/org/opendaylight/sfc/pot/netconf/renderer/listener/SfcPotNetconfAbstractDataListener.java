/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class SfcPotNetconfAbstractDataListener implements DataChangeListener {
    private DataBroker dataBroker;
    private InstanceIdentifier<?> instanceIdentifier;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    private LogicalDatastoreType dataStoreType;

    public SfcPotNetconfAbstractDataListener() {
        this.dataStoreType = LogicalDatastoreType.CONFIGURATION;
        dataBroker = null;
        dataChangeListenerRegistration = null;
    }

    protected void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    protected void setDataStoreType(LogicalDatastoreType dataStoreType) {
        this.dataStoreType = dataStoreType;
    }

    protected void setInstanceIdentifier(InstanceIdentifier<?> instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    public void registerAsDataChangeListener() {
        if (dataBroker != null) {
            dataChangeListenerRegistration =
                    dataBroker.registerDataChangeListener(dataStoreType,
                            instanceIdentifier, this, DataBroker.DataChangeScope.SUBTREE);
        }
    }

    public void closeDataChangeListener() {
        if (dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
        }
    }
}
