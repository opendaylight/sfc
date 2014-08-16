/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import org.junit.Before;
import org.junit.Test;

public class SfcProviderSffFlowWriterTest extends BaseSfcFlowTest {

    private String sffNodeName;
    private SfcProviderSffFlowWriter fw;

    @Before
    public void setup() {
        super.setup();
        sffNodeName = "nodeName";
        fw = new SfcProviderSffFlowWriter(dataBroker);
        fw.setNodeInfo(sffNodeName);
    }

    @Test
    public void testWriteSffNextHop() throws Exception {
        int outPort = 70;
        int inPort = 35;
        int sfpId = 1234;

        String srcMac = "AA:BB:CC:AA:BB:CC";
        String dstMac = "CC:BB:AA:CC:BB:AA";

        fw.writeSffNextHop(inPort, sfpId, srcMac, dstMac, outPort);
        waitForThreadToFinish();

        expectPutTransaction();
        expectNode(sffNodeName);

        expectPutTransaction();
        expectFlow();
        assertFlowMetadata(sfpId);
        assertInPort(inPort);
        assertEthrTypeMatch(0x0800);
        assertFlowActionSetDlDst(dstMac, 0);
        assertFlowActionSetDlSrc(srcMac, 1);
        assertFlowActionOutput(outPort);

    }

    private void waitForThreadToFinish() throws InterruptedException {
        Thread.sleep(500);
    }
}
