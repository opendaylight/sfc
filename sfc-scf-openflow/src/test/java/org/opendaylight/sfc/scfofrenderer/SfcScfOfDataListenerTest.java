/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SfcScfOfDataListenerTest {

    private SfcScfOfDataListener listener;
    private BindingAwareBroker broker;
    private DataBroker dataProvider;
    private SfcScfOfProcessor sfcScfOfProcessor;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    private ServiceFunctionClassifier entryValue;
    private InstanceIdentifier<ServiceFunctionClassifier> entryKey;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);
        listenerRegistration = mock(ListenerRegistration.class);
        change = mock(AsyncDataChangeEvent.class);
        sfcScfOfProcessor = mock(SfcScfOfProcessor.class);

        when(dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataChangeListener.class),
                any(DataChangeScope.class)))
           .thenReturn(listenerRegistration);

        entryValue = mock(ServiceFunctionClassifier.class);
        entryKey = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
            .child(ServiceFunctionClassifier.class)
            .build();

        listener = new SfcScfOfDataListener(dataProvider, sfcScfOfProcessor);
    }

    @Test
    public void onDataChangedCreatedData() {
        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(change.getCreatedData()).thenReturn(entrySet);
        when(sfcScfOfProcessor.createdServiceFunctionClassifier(any(ServiceFunctionClassifier.class))).thenReturn(true);
        listener.onDataChanged(change);
        ServiceFunctionClassifier scf = (ServiceFunctionClassifier) entryValue;
        verify(sfcScfOfProcessor).createdServiceFunctionClassifier(scf);
    }

    @Test
    public void onDataChangedUpdatedData() {
        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(change.getUpdatedData()).thenReturn(entrySet);
        listener.onDataChanged(change);
    }

    @Test
    public void onDataChangedRemovedPaths() {
        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(change.getOriginalData()).thenReturn(entrySet);
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        removedPaths.add(entryKey);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
        when(sfcScfOfProcessor.deletedServiceFunctionClassifier(any(ServiceFunctionClassifier.class))).thenReturn(true);
        listener.onDataChanged(change);
        ServiceFunctionClassifier scf = (ServiceFunctionClassifier) entryValue;
        verify(sfcScfOfProcessor).deletedServiceFunctionClassifier(scf);
    }
}
