/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusIfStateReader;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusBoundServiceWriter;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusDpnIfWriter;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusTsaWriter;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusServiceHandlerTest {

    @Mock
    SfcGeniusIfStateReader sfcGeniusIfStateReader;

    @Mock
    SfcGeniusDpnIfWriter sfcGeniusDpnIfWriter;

    @Mock
    SfcGeniusTsaWriter sfcGeniusTsaWriter;

    @Mock
    SfcGeniusBoundServiceWriter sfcGeniusBoundServiceWriter;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusServiceHandler sfcGeniusServiceHandler;

    @Before
    public void setup() {
        when(sfcGeniusServiceHandler.getBoundServiceWriter()).thenReturn(sfcGeniusBoundServiceWriter);
        when(sfcGeniusServiceHandler.getDpnIfWriter()).thenReturn(sfcGeniusDpnIfWriter);
        when(sfcGeniusServiceHandler.getIfStateReader()).thenReturn(sfcGeniusIfStateReader);
        when(sfcGeniusServiceHandler.getTsaWriter()).thenReturn(sfcGeniusTsaWriter);
        when(sfcGeniusTsaWriter.createTerminatingServiceAction(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(sfcGeniusTsaWriter.removeTerminatingServiceAction(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(sfcGeniusBoundServiceWriter.bindService(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sfcGeniusBoundServiceWriter.unbindService(any())).thenReturn(CompletableFuture.completedFuture(null));
    }


    @Test
    public void bindToInterfaceFirstOfDpn() throws Exception {
        String interfaceName = "IF1";
        BigInteger dpnId = BigInteger.valueOf(17);
        Short offset = 130;

        when(sfcGeniusIfStateReader.readDpnId(interfaceName)).thenReturn(CompletableFuture.completedFuture(dpnId));
        when(sfcGeniusDpnIfWriter.addInterface(dpnId, interfaceName))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(dpnId)));

        sfcGeniusServiceHandler.bindToInterface(interfaceName);

        verify(sfcGeniusTsaWriter).createTerminatingServiceAction(dpnId);
        verify(sfcGeniusBoundServiceWriter).bindService(interfaceName);
    }

    @Test
    public void bindToInterface() throws Exception {
        String interfaceName = "IF1";
        BigInteger dpnId = BigInteger.valueOf(18);
        Short offset = 130;

        when(sfcGeniusIfStateReader.readDpnId(interfaceName)).thenReturn(CompletableFuture.completedFuture(dpnId));
        when(sfcGeniusDpnIfWriter.addInterface(dpnId, interfaceName))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        sfcGeniusServiceHandler.bindToInterface(interfaceName);

        verify(sfcGeniusBoundServiceWriter).bindService(interfaceName);
        verifyZeroInteractions(sfcGeniusTsaWriter);
    }

    @Test
    public void unbindFromInterfaceLastOfDpn() throws Exception {
        String interfaceName = "IF1";
        BigInteger dpnId = BigInteger.valueOf(19);

        when(sfcGeniusIfStateReader.readDpnId(interfaceName)).thenReturn(CompletableFuture.completedFuture(dpnId));
        when(sfcGeniusDpnIfWriter.removeInterfaceFromDpn(dpnId, interfaceName))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(dpnId)));

        sfcGeniusServiceHandler.unbindFromInterface(interfaceName);

        verify(sfcGeniusTsaWriter).removeTerminatingServiceAction(dpnId);
        verify(sfcGeniusBoundServiceWriter).unbindService(interfaceName);
    }

    @Test
    public void unbindFromInterface() throws Exception {
        String interfaceName = "IF1";
        BigInteger dpnId = BigInteger.valueOf(19);

        when(sfcGeniusIfStateReader.readDpnId(interfaceName)).thenReturn(CompletableFuture.completedFuture(dpnId));
        when(sfcGeniusDpnIfWriter.removeInterfaceFromDpn(dpnId, interfaceName))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        sfcGeniusServiceHandler.unbindFromInterface(interfaceName);

        verify(sfcGeniusBoundServiceWriter).unbindService(interfaceName);
        verifyZeroInteractions(sfcGeniusTsaWriter);
    }

    @Test
    public void interfaceStateUp() throws Exception {

    }

    @Test
    public void interfaceStateDown() throws Exception {

    }

}
