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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePathBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusSfStateReaderTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusSfStateReader sfcGeniusSfStateReader;

    @Mock
    ServiceFunctionState serviceFunctionState;

    @Test
    public void readSfpNames() throws Exception {
        SfName sfName = new SfName("SF1");
        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, new ServiceFunctionStateKey(sfName)).build();
        List<SfServicePath> sfServicePaths = Arrays.asList(
                new SfServicePathBuilder().setName(new SfpName("SFP1")).build(),
                new SfServicePathBuilder().setName(new SfpName("SFP2")).build()
        );
        when(serviceFunctionState.getSfServicePath()).thenReturn(sfServicePaths);
        doReturn(CompletableFuture.completedFuture(Optional.of(serviceFunctionState)))
                .when(sfcGeniusSfStateReader).doReadOptional(LogicalDatastoreType.OPERATIONAL, sfStateIID);
        List<SfpName> sfpNameList = sfcGeniusSfStateReader.readSfpNames(sfName).get();
        assertThat(sfpNameList.size(), is(2));
        assertThat(sfpNameList, containsInAnyOrder(new SfpName("SFP1"), new SfpName("SFP2")));
    }

    @Test
    public void readSfpNamesEmpty() throws Exception {
        SfName sfName = new SfName("SF1");
        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, new ServiceFunctionStateKey(sfName)).build();
        doReturn(CompletableFuture.completedFuture(Optional.empty()))
                .when(sfcGeniusSfStateReader).doReadOptional(LogicalDatastoreType.OPERATIONAL, sfStateIID);
        List<SfpName> sfpNameList = sfcGeniusSfStateReader.readSfpNames(sfName).get();
        assertThat(sfpNameList.size(), is(0));
    }

}
