/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the OVSDB southbound operational datastore
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2015-02-13
 */

package org.opendaylight.sfc.sfc_ovs.provider.listener;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class SfcOvsAbstractDataListener implements DataChangeListener {
    protected ExecutorService executor;
    protected DataBroker dataBroker;
    protected InstanceIdentifier<?> instanceIdentifier;
    protected ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    protected LogicalDatastoreType dataStoreType;

    public SfcOvsAbstractDataListener() {
        this.dataStoreType = LogicalDatastoreType.CONFIGURATION;
    }

    public ExecutorService getExecutorService() {
        return executor;
    }

    public void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setDataStoreType(LogicalDatastoreType dataStoreType) {
        this.dataStoreType = dataStoreType;
    }

    public void setInstanceIdentifier(InstanceIdentifier<?> instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    public ListenerRegistration<DataChangeListener> getDataChangeListenerRegistration() {
        return dataChangeListenerRegistration;
    }

    public void registerAsDataChangeListener(DataBroker.DataChangeScope scope) {
        dataChangeListenerRegistration =
                dataBroker.registerDataChangeListener(dataStoreType,
                        instanceIdentifier, this, scope);
    }

    public void closeDataChangeListener() {
        dataChangeListenerRegistration.close();
    }


}
