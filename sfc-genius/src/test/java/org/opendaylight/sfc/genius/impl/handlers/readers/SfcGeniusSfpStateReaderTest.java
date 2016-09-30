/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePathBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfpStateReaderTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusSfpStateReader sfpStateReader;

    @Mock
    ServiceFunctionPathState serviceFunctionPathState;

    @Test
    public void readRspNames() throws Exception {
        SfpName sfpName = new SfpName("SFP1");
        InstanceIdentifier<ServiceFunctionPathState> sfpStateIID;
        sfpStateIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpName))
                .build();
        List<SfpRenderedServicePath> renderedServicePaths = Arrays.asList(
                new SfpRenderedServicePathBuilder().setName(new RspName("RSP1")).build(),
                new SfpRenderedServicePathBuilder().setName(new RspName("RSP2")).build());
        when(serviceFunctionPathState.getSfpRenderedServicePath()).thenReturn(renderedServicePaths);
        doReturn(CompletableFuture.completedFuture(Optional.of(serviceFunctionPathState)))
                .when(sfpStateReader).doReadOptional(LogicalDatastoreType.OPERATIONAL, sfpStateIID);
        List<RspName> rspNamesList  = sfpStateReader.readRspNames(sfpName).get();
        assertThat(rspNamesList, containsInAnyOrder(new RspName("RSP1"), new RspName("RSP2")));
        assertThat(rspNamesList.size(), is(2));
    }

    @Test
    public void readRspNamesEmpty() throws Exception {
        SfpName sfpName = new SfpName("SFP1");
        InstanceIdentifier<ServiceFunctionPathState> sfpStateIID;
        sfpStateIID = InstanceIdentifier.builder(ServiceFunctionPathsState.class)
                .child(ServiceFunctionPathState.class, new ServiceFunctionPathStateKey(sfpName))
                .build();
        List<SfpRenderedServicePath> renderedServicePaths = Arrays.asList(
                new SfpRenderedServicePathBuilder().setName(new RspName("RSP1")).build(),
                new SfpRenderedServicePathBuilder().setName(new RspName("RSP2")).build());
        when(serviceFunctionPathState.getSfpRenderedServicePath()).thenReturn(renderedServicePaths);
        doReturn(CompletableFuture.completedFuture(Optional.empty()))
                .when(sfpStateReader).doReadOptional(LogicalDatastoreType.OPERATIONAL, sfpStateIID);
        List<RspName> rspNamesList  = sfpStateReader.readRspNames(sfpName).get();
        assertThat(rspNamesList.size(), is(0));
    }

}
