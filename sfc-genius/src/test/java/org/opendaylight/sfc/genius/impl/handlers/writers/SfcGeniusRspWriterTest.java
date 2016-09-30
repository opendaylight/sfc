/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.writers;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class SfcGeniusRspWriterTest {

    private SfcGeniusRspWriter sfcGeniusRspWriter;

    @Mock
    RenderedServicePath renderedServicePath;

    @Mock
    WriteTransaction writeTransaction;

    @Before
    public void setup() {
        sfcGeniusRspWriter = new SfcGeniusRspWriter(writeTransaction);
        when(renderedServicePath.getKey()).thenReturn(new RenderedServicePathKey(new RspName("RSP1")));
    }

    @Test
    public void deleteRsp() throws Exception {
        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePath.getKey())
                .build();
        RenderedServicePath renderedServicePath1 = sfcGeniusRspWriter.deleteRsp(renderedServicePath).get();
        assertThat(renderedServicePath1, sameInstance(renderedServicePath));
        verify(writeTransaction).delete(LogicalDatastoreType.OPERATIONAL, rspIID);
    }

    @Test
    public void createRsp() throws Exception {
        InstanceIdentifier<RenderedServicePath> rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, renderedServicePath.getKey())
                .build();
        sfcGeniusRspWriter.createRsp(renderedServicePath).get();
        verify(writeTransaction).put(LogicalDatastoreType.OPERATIONAL, rspIID, renderedServicePath);
    }

}
