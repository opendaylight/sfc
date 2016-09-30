/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfpStatesReaderTest {

    @Mock
    SfcGeniusSfpStateReader sfcGeniusSfpStateReader;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SfcGeniusSfpStatesReader reader;

    @Before
    public void setup() {
        when(reader.getSfpStateReader()).thenReturn(sfcGeniusSfpStateReader);
    }

    @Test
    public void readRspNames() throws Exception {
        when(sfcGeniusSfpStateReader.readRspNames(new SfpName("SFP1")))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(new RspName("RSP1"), new RspName("RSP2"))));
        when(sfcGeniusSfpStateReader.readRspNames(new SfpName("SFP2")))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(new RspName("RSP3"), new RspName("RSP1"))));

        CompletableFuture<List<RspName>> future = reader.readRspNames(
                Arrays.asList(new SfpName("SFP1"), new SfpName("SFP2")));

        List<RspName> rspNames = future.get();

        assertThat(rspNames, containsInAnyOrder(new RspName("RSP1"), new RspName("RSP2"), new RspName("RSP3")));
        assertTrue(rspNames.size() == 3);
    }

    @Test
    public void readRspNamesEmpty() throws Exception {
        when(sfcGeniusSfpStateReader.readRspNames(new SfpName("SFP1")))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(sfcGeniusSfpStateReader.readRspNames(new SfpName("SFP2")))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        CompletableFuture<List<RspName>> future = reader.readRspNames(
                Arrays.asList(new SfpName("SFP1"), new SfpName("SFP2")));

        List<RspName> rspNames = future.get();

        assertTrue(rspNames.size() == 0);
    }

}
