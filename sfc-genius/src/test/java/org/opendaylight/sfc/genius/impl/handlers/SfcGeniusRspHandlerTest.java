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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusRspReader;
import org.opendaylight.sfc.genius.impl.handlers.readers.SfcGeniusSfStatesReader;
import org.opendaylight.sfc.genius.impl.handlers.writers.SfcGeniusRspWriter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusRspHandlerTest {

    @Mock
    SfcGeniusRspWriter sfcGeniusRspWriter;

    @Mock
    SfcGeniusRspReader sfcGeniusRspReader;

    @Mock
    SfcGeniusSfStatesReader sfcGeniusSfStatesReader;

    @Mock
    RenderedServicePath renderedServicePath;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusRspHandler sfcGeniusRspHandler;

    @Before
    public void setup() {
        when(sfcGeniusRspHandler.getSfStatesReader()).thenReturn(sfcGeniusSfStatesReader);
        when(sfcGeniusRspHandler.getRspReader()).thenReturn(sfcGeniusRspReader);
        when(sfcGeniusRspHandler.getRspWriter()).thenReturn(sfcGeniusRspWriter);
    }

    @Test
    public void interfaceStateUp() throws Exception {
        String testInterface = "TestInterface";
        List<SfName> sfNameList = Arrays.asList(new SfName("SF1"), new SfName("SF2"));
        List<SfpName> sfpNameList = Arrays.asList(new SfpName("RSP1"), new SfpName("RSP2"));
        List<RspName> rspNameList = Arrays.asList(new RspName("RSP1"), new RspName("RSP2"));
        List<RenderedServicePath> rspList = Arrays.asList(
                new RenderedServicePathBuilder().setName(rspNameList.get(0)).build(),
                new RenderedServicePathBuilder().setName(rspNameList.get(1)).build()
        );

        when(sfcGeniusSfStatesReader.readSfpNames(sfNameList))
                .thenReturn(CompletableFuture.completedFuture(sfpNameList));
        when(sfcGeniusRspReader.readRsp(rspNameList.get(0)))
                .thenReturn(CompletableFuture.completedFuture(rspList.get(0)));
        when(sfcGeniusRspReader.readRsp(rspNameList.get(1)))
                .thenReturn(CompletableFuture.completedFuture(rspList.get(1)));
        when(sfcGeniusRspWriter.deleteRsp(rspList.get(0)))
                .thenReturn(CompletableFuture.completedFuture(rspList.get(0)));
        when(sfcGeniusRspWriter.deleteRsp(rspList.get(1)))
                .thenReturn(CompletableFuture.completedFuture(rspList.get(1)));
        when(sfcGeniusRspWriter.createRsp(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        sfcGeniusRspHandler.interfaceStateUp(testInterface, sfNameList);

        verify(sfcGeniusRspWriter).deleteRsp(rspList.get(0));
        verify(sfcGeniusRspWriter).createRsp(rspList.get(0));
        verify(sfcGeniusRspWriter).deleteRsp(rspList.get(1));
        verify(sfcGeniusRspWriter).createRsp(rspList.get(1));
    }

}
