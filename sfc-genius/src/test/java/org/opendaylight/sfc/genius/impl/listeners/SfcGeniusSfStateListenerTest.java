/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
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
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.handlers.ISfcGeniusInterfaceServiceHandler;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfStateListenerTest {

    @Mock
    ISfcGeniusInterfaceServiceHandler iSfcGeniusInterfaceServiceHandler;

    @Mock
    Executor executor;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Mock
    ServiceFunctionState serviceFunctionState;

    @Mock
    List<SfServicePath> sfpList;

    SfcGeniusSfStateListener sfcGeniusSfStateListener;

    @Before
    public void setup() {
        sfcGeniusSfStateListener = new SfcGeniusSfStateListener(iSfcGeniusInterfaceServiceHandler, executor);
    }

    @Test
    public void remove() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1);
        sfcGeniusSfStateListener.remove(null, serviceFunctionState);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(iSfcGeniusInterfaceServiceHandler).unbindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void removeNoPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0);
        sfcGeniusSfStateListener.remove(null, serviceFunctionState);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(iSfcGeniusInterfaceServiceHandler);
    }

    @Test
    public void updateMorePaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1).thenReturn(0);
        sfcGeniusSfStateListener.update(null, serviceFunctionState, serviceFunctionState);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(iSfcGeniusInterfaceServiceHandler).bindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void updateLessPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1).thenReturn(2);
        sfcGeniusSfStateListener.update(null, serviceFunctionState, serviceFunctionState);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(iSfcGeniusInterfaceServiceHandler);
    }

    @Test
    public void updateZeroPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0).thenReturn(1);
        sfcGeniusSfStateListener.update(null, serviceFunctionState, serviceFunctionState);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(iSfcGeniusInterfaceServiceHandler).unbindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void updateEqualPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0).thenReturn(0);
        sfcGeniusSfStateListener.update(null, serviceFunctionState, serviceFunctionState);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(iSfcGeniusInterfaceServiceHandler);
    }

    @Test
    public void add() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(1);
        sfcGeniusSfStateListener.add(null, serviceFunctionState);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(iSfcGeniusInterfaceServiceHandler).bindInterfacesOfServiceFunction("SF1");
    }

    @Test
    public void addNoPaths() throws Exception {
        when(serviceFunctionState.getName()).thenReturn(new SfName("SF1"));
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfpList);
        when(sfpList.size()).thenReturn(0);
        sfcGeniusSfStateListener.add(null, serviceFunctionState);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(iSfcGeniusInterfaceServiceHandler);
    }

}
