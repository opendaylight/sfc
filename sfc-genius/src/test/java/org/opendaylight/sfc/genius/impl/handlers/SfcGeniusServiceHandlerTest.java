/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusBoundServiceWriter;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusTsaWriter;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusServiceHandlerTest {

    @Mock
    SfcGeniusTsaWriter sfcGeniusTsaWriter;

    @Mock
    SfcGeniusBoundServiceWriter sfcGeniusBoundServiceWriter;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusServiceHandler sfcGeniusServiceHandler;

    @Before
    public void setup() {
        when(sfcGeniusServiceHandler.getBoundServiceWriter()).thenReturn(sfcGeniusBoundServiceWriter);
        when(sfcGeniusServiceHandler.getTsaWriter()).thenReturn(sfcGeniusTsaWriter);
        when(sfcGeniusBoundServiceWriter.bindService(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sfcGeniusBoundServiceWriter.unbindService(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void bindToInterface() throws Exception {
        String interfaceName = "IF1";
        sfcGeniusServiceHandler.bindToInterface(interfaceName);
        verify(sfcGeniusBoundServiceWriter).bindService(interfaceName);
    }

    @Test
    public void unbindFromInterface() throws Exception {
        String interfaceName = "IF1";
        sfcGeniusServiceHandler.unbindFromInterface(interfaceName);
        verify(sfcGeniusBoundServiceWriter).unbindService(interfaceName);
    }

    @Test
    public void bindToNode() throws Exception {
        Uint64 dpnId = Uint64.valueOf(18);
        sfcGeniusServiceHandler.bindToNode(dpnId);
        verify(sfcGeniusTsaWriter).createTerminatingServiceAction(dpnId);
    }

    @Test
    public void unbindFromNode() throws Exception {
        Uint64 dpnId = Uint64.valueOf(18);
        sfcGeniusServiceHandler.unbindFromNode(dpnId);
        verify(sfcGeniusTsaWriter).removeTerminatingServiceAction(dpnId);
    }
}
