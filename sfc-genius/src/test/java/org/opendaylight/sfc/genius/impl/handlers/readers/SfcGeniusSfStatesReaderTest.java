/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfStatesReaderTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusSfStatesReader sfcGeniusSfStatesReader;

    @Mock
    SfcGeniusSfStateReader sfcGeniusSfStateReader;

    @Test
    public void readSfpNames() throws Exception {
        when(sfcGeniusSfStateReader.readSfpNames(new SfName("SF1"))).thenReturn(
                CompletableFuture.completedFuture(Arrays.asList(new SfpName("SFP1"), new SfpName("SFP2")))
        );
        when(sfcGeniusSfStateReader.readSfpNames(new SfName("SF2"))).thenReturn(
                CompletableFuture.completedFuture(Arrays.asList(new SfpName("SFP2"), new SfpName("SFP3")))
        );
        doReturn(sfcGeniusSfStateReader).when(sfcGeniusSfStatesReader).getSfStateReader();
        List<SfpName> sfpNames =
                sfcGeniusSfStatesReader.readSfpNames(Arrays.asList(new SfName("SF1"), new SfName("SF2"))).get();
        assertThat(sfpNames.size(), is(3));
        assertThat(sfpNames, containsInAnyOrder(new SfpName("SFP1"), new SfpName("SFP2"), new SfpName("SFP3")));
    }

    @Test
    public void readSfpNamesEmpty() throws Exception {
        when(sfcGeniusSfStateReader.readSfpNames(new SfName("SF1"))).thenReturn(
                CompletableFuture.completedFuture(Collections.emptyList())
        );
        doReturn(sfcGeniusSfStateReader).when(sfcGeniusSfStatesReader).getSfStateReader();
        List<SfpName> sfpNames =
                sfcGeniusSfStatesReader.readSfpNames(Collections.singletonList(new SfName("SF1"))).get();
        assertThat(sfpNames.size(), is(0));
    }
}
