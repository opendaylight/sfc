/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfStateListenerTest {

    @Mock
    private DataBroker dataBroker;

    @Mock
    private SfcGeniusServiceManager sfcGeniusServiceManager;

    @Mock
    private ExecutorService executorService;

    @Mock
    private ServiceFunctionState serviceFunctionState;

    @Mock
    private List<SfServicePath> sfpList;

    private SfcGeniusSfStateListener sfcGeniusSfStateListener;

    @Before
    public void setup() {
        sfcGeniusSfStateListener = new SfcGeniusSfStateListener(dataBroker, sfcGeniusServiceManager, executorService);
    }

    @Test
    public void remove() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1);
        sfcGeniusSfStateListener.remove(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState);
        verify(sfcGeniusServiceManager).unbindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void removeNoPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0);
        sfcGeniusSfStateListener.remove(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void updateMorePaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1).thenReturn(0);
        sfcGeniusSfStateListener.update(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState,
                                        serviceFunctionState);
        verify(sfcGeniusServiceManager).bindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void updateLessPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1).thenReturn(2);
        sfcGeniusSfStateListener.update(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState,
                                        serviceFunctionState);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void updateZeroPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0).thenReturn(1);
        sfcGeniusSfStateListener.update(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState,
                                        serviceFunctionState);
        verify(sfcGeniusServiceManager).unbindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void updateEqualPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0).thenReturn(0);
        sfcGeniusSfStateListener.update(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState,
                                        serviceFunctionState);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }

    @Test
    public void add() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1);
        sfcGeniusSfStateListener.add(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState);
        verify(sfcGeniusServiceManager).bindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void addNoPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0);
        sfcGeniusSfStateListener.add(InstanceIdentifier.create(ServiceFunctionState.class), serviceFunctionState);
        verifyZeroInteractions(sfcGeniusServiceManager);
    }
}
