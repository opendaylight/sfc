/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.sfc.genius.impl.utils.SfcGeniusConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusBoundServiceWriterTest {

    @Mock
    WriteTransaction writeTransaction;

    @Mock
    Executor executor;

    @Captor
    ArgumentCaptor<InstanceIdentifier<BoundServices>> instanceIdentifierCaptor;

    @Captor
    ArgumentCaptor<BoundServices> boundServicesCaptor;

    private SfcGeniusBoundServiceWriter writer;

    @Before
    public void setup() {
        writer = new SfcGeniusBoundServiceWriter(writeTransaction);
    }

    @Test
    public void bindService() throws Exception {
        CompletableFuture<Void> future = writer.bindService("IF1");

        verify(writeTransaction).put(
                eq(LogicalDatastoreType.CONFIGURATION),
                instanceIdentifierCaptor.capture(),
                boundServicesCaptor.capture());

        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(false));
        assertThat(future.isCancelled(), is(false));

        InstanceIdentifier<BoundServices> iid = instanceIdentifierCaptor.getValue();
        assertThat(iid.firstKeyOf(ServicesInfo.class), is(new ServicesInfoKey("IF1", ServiceModeIngress.class)));
        assertThat(iid.firstKeyOf(BoundServices.class), is(new BoundServicesKey(NwConstants.SFC_SERVICE_INDEX)));

        BoundServices boundServices = boundServicesCaptor.getValue();
        assertThat(boundServices.getServiceName(), is(NwConstants.SFC_SERVICE_NAME));
        assertThat(boundServices.getServicePriority(), is(NwConstants.SFC_SERVICE_INDEX));
        assertThat(boundServices.getServiceType(), is(equalTo(ServiceTypeFlowBased.class)));

        StypeOpenflow stypeOpenflow = boundServices.getAugmentation(StypeOpenflow.class);
        assertThat(stypeOpenflow.getFlowCookie(), is(SfcGeniusConstants.COOKIE_SFC_INGRESS_TABLE));
        assertThat(stypeOpenflow.getFlowPriority(), is(SfcGeniusConstants.SFC_SERVICE_PRIORITY));
        assertThat(stypeOpenflow.getInstruction().size(), is(1));

        Instruction instruction = stypeOpenflow.getInstruction().get(0);
        assertThat(instruction.getInstruction(), is(instanceOf(GoToTableCase.class)));

        GoToTableCase goToTable = (GoToTableCase) instruction.getInstruction();
        assertThat(goToTable.getGoToTable().getTableId(), is(NwConstants.SFC_TRANSPORT_INGRESS_TABLE));
    }

    @Test
    public void unbindService() throws Exception {
        CompletableFuture<Void> future = writer.unbindService("IF2");

        verify(writeTransaction).delete(eq(LogicalDatastoreType.CONFIGURATION), instanceIdentifierCaptor.capture());

        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(false));
        assertThat(future.isCancelled(), is(false));

        InstanceIdentifier<BoundServices> iid = instanceIdentifierCaptor.getValue();
        assertThat(iid.firstKeyOf(ServicesInfo.class), is(new ServicesInfoKey("IF2", ServiceModeIngress.class)));
        assertThat(iid.firstKeyOf(BoundServices.class), is(new BoundServicesKey(NwConstants.SFC_SERVICE_INDEX)));
    }

}
