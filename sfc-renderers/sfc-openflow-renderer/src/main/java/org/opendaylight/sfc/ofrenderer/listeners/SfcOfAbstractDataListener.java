/**
 * Copyright (c) 2014, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class SfcOfAbstractDataListener<T extends DataObject> implements DataTreeChangeListener<T> {

    private DataBroker dataBroker;
    private InstanceIdentifier<T> instanceIdentifier;
    private ListenerRegistration<SfcOfAbstractDataListener<T>> dataChangeListenerRegistration;

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setIID(InstanceIdentifier<T> instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void registerAsDataChangeListener(LogicalDatastoreType datastoreType) {
        dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                datastoreType, instanceIdentifier), this);
    }

    public void registerAsDataChangeListener() {
        registerAsDataChangeListener(LogicalDatastoreType.CONFIGURATION);
    }

    public void closeDataChangeListener() {
        dataChangeListenerRegistration.close();
    }
}
