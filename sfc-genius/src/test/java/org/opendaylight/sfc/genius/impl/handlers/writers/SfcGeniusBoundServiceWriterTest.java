/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusBoundServiceWriterTest {

    @Mock
    WriteTransaction writeTransaction;

    @Captor
    ArgumentCaptor<InstanceIdentifier<BoundServices>> instanceIdentifierCaptor;

    private SfcGeniusBoundServiceWriter writer;

    @Before
    public void setup() {
        writer = new SfcGeniusBoundServiceWriter(writeTransaction);
    }

    @Test
    public void unbindService() throws Exception {
        CompletableFuture<Void> future = writer.unbindService("IF2");

        verify(writeTransaction).delete(eq(LogicalDatastoreType.CONFIGURATION), instanceIdentifierCaptor.capture());

        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(false));
        assertThat(future.isCancelled(), is(false));

        InstanceIdentifier iid = instanceIdentifierCaptor.getValue();
        assertThat(iid.firstKeyOf(ServicesInfo.class), is(new ServicesInfoKey("IF2", ServiceModeIngress.class)));
        assertThat(iid.firstKeyOf(BoundServices.class), is(new BoundServicesKey(NwConstants.SCF_SERVICE_INDEX)));
    }

}
