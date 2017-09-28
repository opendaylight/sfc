/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * DataChangeListener attached to the Netconf Topology
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2015-02-13
 */

package org.opendaylight.sfc.netconf.provider.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class SfcNetconfAbstractDataListener<T extends DataObject> implements DataTreeChangeListener<T>,
        AutoCloseable {
    private final ListenerRegistration<SfcNetconfAbstractDataListener<T>> dataChangeListenerRegistration;

    public SfcNetconfAbstractDataListener(DataBroker dataBroker, InstanceIdentifier<T> instanceIdentifier,
            LogicalDatastoreType dataStoreType) {
        dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                dataStoreType, instanceIdentifier), this);
    }

    @Override
    public void close() {
        dataChangeListenerRegistration.close();
    }
}
