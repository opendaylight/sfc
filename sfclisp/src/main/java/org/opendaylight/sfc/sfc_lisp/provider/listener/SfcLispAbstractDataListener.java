/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_lisp.provider.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class SfcLispAbstractDataListener implements DataChangeListener {
    protected OpendaylightSfc odlSfc;
    protected DataBroker dataBroker;
    protected ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    protected LogicalDatastoreType dataStoreType;
    protected InstanceIdentifier<?> instanceId;
    public SfcLispAbstractDataListener() {
        this.dataStoreType = LogicalDatastoreType.CONFIGURATION;
    }

    public void setOpendaylightSfc(OpendaylightSfc odlSfc) {
        this.odlSfc = odlSfc;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setDataStoreType(LogicalDatastoreType dataStoreType) {
        this.dataStoreType = dataStoreType;
    }

    public void setInstanceIdentifier(InstanceIdentifier<?> instanceId) {
        this.instanceId = instanceId;
    }

    public void registerAsDataChangeListener() {
        dataChangeListenerRegistration =  dataBroker.registerDataChangeListener(dataStoreType, instanceId, this,
                DataBroker.DataChangeScope.SUBTREE);
    }

    public void closeDataChangeListener() {
        dataChangeListenerRegistration.close();
    }
}
