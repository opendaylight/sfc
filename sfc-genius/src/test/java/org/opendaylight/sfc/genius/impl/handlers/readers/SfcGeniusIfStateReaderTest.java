/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusIfStateReaderTest {

    @Mock
    Interface anInterface;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    SfcGeniusIfStateReader reader;

    @Captor
    ArgumentCaptor<InstanceIdentifier<Interface>> instanceIdentifierCaptor;

    @Test
    public void readDpnId() throws Exception {
        String interfaceName = "IF1";
        List<String> lowerLayerIfList = Collections.singletonList("openflow:1234567890:1");
        when(anInterface.getLowerLayerIf()).thenReturn(lowerLayerIfList);
        doReturn(CompletableFuture.completedFuture(anInterface))
                .when(reader).doRead(eq(LogicalDatastoreType.OPERATIONAL), instanceIdentifierCaptor.capture());

        BigInteger readDpnId = reader.readDpnId(interfaceName).get();

        assertThat(instanceIdentifierCaptor.getValue().firstKeyOf(Interface.class).getName(), is(interfaceName));
        assertThat(readDpnId, is(BigInteger.valueOf(1234567890)));
    }

}
