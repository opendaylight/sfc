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

import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.handlers.ISfcGeniusInterfaceServiceHandler;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusInterfaceStateListenerTest {

    @Mock
    ISfcGeniusInterfaceServiceHandler handler;

    @Mock
    Executor executor;

    @Mock
    Interface anInterface;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    SfcGeniusInterfaceStateListener sfcGeniusInterfaceStateListener;

    @Before
    public void setup() {
        when(anInterface.getName()).thenReturn("IF1");
        when(anInterface.getLowerLayerIf()).thenReturn(Collections.singletonList("openflow:123456789:3"));
        sfcGeniusInterfaceStateListener = new SfcGeniusInterfaceStateListener(handler, executor);
    }

    @Test
    public void remove() throws Exception {
        sfcGeniusInterfaceStateListener.remove(null, anInterface);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(handler).interfaceStateDown("IF1", BigInteger.valueOf(123456789));
    }

    @Test
    public void removeNoLowerLayerIf() throws Exception {
        when(anInterface.getLowerLayerIf()).thenReturn(Collections.emptyList());
        sfcGeniusInterfaceStateListener.remove(null, anInterface);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(handler);
    }

    @Test
    public void add() throws Exception {
        sfcGeniusInterfaceStateListener.add(null, anInterface);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(handler).interfaceStateUp("IF1", BigInteger.valueOf(123456789));
    }

    @Test
    public void addNoLowerLayerIf() throws Exception {
        when(anInterface.getLowerLayerIf()).thenReturn(Collections.emptyList());
        sfcGeniusInterfaceStateListener.add(null, anInterface);
        verifyZeroInteractions(executor);
        verifyZeroInteractions(handler);
    }

}
