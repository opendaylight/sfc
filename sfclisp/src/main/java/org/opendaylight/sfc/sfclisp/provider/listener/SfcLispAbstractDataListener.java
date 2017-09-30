/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfclisp.provider.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class SfcLispAbstractDataListener<T extends DataObject> implements DataTreeChangeListener<T>,
        AutoCloseable {
    private final ListenerRegistration<SfcLispAbstractDataListener<T>> dataChangeListenerRegistration;

    public SfcLispAbstractDataListener(DataBroker dataBroker, InstanceIdentifier<T> instanceId,
            LogicalDatastoreType dataStoreType) {
        dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                dataStoreType, instanceId), this);
    }

    @Override
    public void close() {
        dataChangeListenerRegistration.close();
    }
}
