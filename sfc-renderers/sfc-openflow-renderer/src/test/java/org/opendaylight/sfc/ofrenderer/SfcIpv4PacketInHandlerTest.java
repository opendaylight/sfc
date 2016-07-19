/*
 * Copyright (c) 2014, 2015 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.sfc.ofrenderer.openflow.SfcIpv4PacketInHandler;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcIpv4PacketInHandlerTest {

    SfcIpv4PacketInHandler pktInHandler;
    SfcOfFlowProgrammerImpl flowProgrammerMock;

    public SfcIpv4PacketInHandlerTest() {
        this.flowProgrammerMock = mock(SfcOfFlowProgrammerImpl.class);
        when(this.flowProgrammerMock.compareClassificationTableCookie((FlowCookie) anyObject())).thenReturn(true);

        this.pktInHandler = new SfcIpv4PacketInHandler(this.flowProgrammerMock);
    }

    @Test
    public void nullInvocation() {
        this.pktInHandler.onPacketReceived(null);
        verifyNoMoreInteractions(this.flowProgrammerMock);
    }

    @Test
    public void pktInFlowsCreated() throws Exception {
        this.pktInHandler.onPacketReceived(createPacket());
        this.pktInHandler.close();

        verify(this.flowProgrammerMock, times(2)).setFlowRspId(anyLong());
        verify(this.flowProgrammerMock, times(2)).
            configurePathMapperAclFlow(anyString(), anyString(), anyString(), anyShort());
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);
    }

    @Test
    public void pktInBuffering() {
        this.pktInHandler.setMaxBufferTime(10000); // 10 seconds
        this.pktInHandler.setPacketCountPurge(1000);
        PacketReceived pkt = createPacket();

        this.pktInHandler.onPacketReceived(pkt);
        verify(this.flowProgrammerMock, times(2)).setFlowRspId(anyLong());
        verify(this.flowProgrammerMock, times(2)).
            configurePathMapperAclFlow(anyString(), anyString(), anyString(), anyShort());
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);

        // When called again, nothing should be called on
        // the FlowProgrammer since the pkt is buffered
        resetFlowProgrammerMock();
        this.pktInHandler.onPacketReceived(pkt);
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);
    }

    @Test
    public void pktInBufferingTimeout() throws InterruptedException {
        this.pktInHandler.setMaxBufferTime(1); // 1 millisecond
        this.pktInHandler.setPacketCountPurge(1000);
        PacketReceived pkt = createPacket();

        this.pktInHandler.onPacketReceived(pkt);
        verify(this.flowProgrammerMock, times(2)).setFlowRspId(anyLong());
        verify(this.flowProgrammerMock, times(2)).
            configurePathMapperAclFlow(anyString(), anyString(), anyString(), anyShort());
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);

        // When called again, the packet should be sent to the FlowProgrammer
        // again, since the timeout is 1 millisecond
        Thread.sleep(10); // sleep 10 milliseconds, to let the buffer time expire
        resetFlowProgrammerMock();
        this.pktInHandler.onPacketReceived(pkt);
        verify(this.flowProgrammerMock, times(2)).setFlowRspId(anyLong());
        verify(this.flowProgrammerMock, times(2)).
            configurePathMapperAclFlow(anyString(), anyString(), anyString(), anyShort());
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);
    }

    @Test
    public void pktInPurgeBuffering() throws InterruptedException {
        this.pktInHandler.setMaxBufferTime(1); // 1 millisecond
        this.pktInHandler.setPacketCountPurge(2);
        PacketReceived pkt = createPacket();

        assertEquals(this.pktInHandler.getBufferSize(), 0);
        this.pktInHandler.onPacketReceived(pkt);
        verify(this.flowProgrammerMock, times(2)).setFlowRspId(anyLong());
        verify(this.flowProgrammerMock, times(2)).
            configurePathMapperAclFlow(anyString(), anyString(), anyString(), anyShort());
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);
        assertEquals(this.pktInHandler.getBufferSize(), 1);

        // When called again, the purgeCount will be exceeded, the buffer will
        // be flushed, and this packet will be added back, making a size of 1
        Thread.sleep(10); // sleep 10 milliseconds, to let the buffer time expire
        resetFlowProgrammerMock();
        this.pktInHandler.onPacketReceived(pkt);
        verify(this.flowProgrammerMock, times(2)).setFlowRspId(anyLong());
        verify(this.flowProgrammerMock, times(2)).
            configurePathMapperAclFlow(anyString(), anyString(), anyString(), anyShort());
        verify(this.flowProgrammerMock, times(1)).compareClassificationTableCookie((FlowCookie) anyObject());
        verifyNoMoreInteractions(this.flowProgrammerMock);
        assertEquals(this.pktInHandler.getBufferSize(), 1);
    }

    // When we want to reset the method call counters,
    // it also resets the stubs, so do both together
    private void resetFlowProgrammerMock() {
        reset(this.flowProgrammerMock);
        when(this.flowProgrammerMock.compareClassificationTableCookie((FlowCookie) anyObject())).thenReturn(true);
    }

    private PacketReceived createPacket() {
        PacketReceived pktMock = mock(PacketReceived.class);

        // Stub the PacketReceived methods

        // getPayload()
        // We dont need a real payload, just the etherType, IpSrc, and IpDst
        // MacSrc=a1a1a1a1a1a1, MacDst=b2b2b2b2b2b2, etherType=0800
        // IpHdrStuff=000000000000000000000000, IpSrc=0a0a0001, IpDst=0b0b0001
        byte[] payload = hexStringToByteArray(
                "a1a1a1a1a1a1b2b2b2b2b2b20800" +
                "0000000000000000000000000" +
                "a0a00010b0b0001");
        when(pktMock.getPayload()).thenReturn(payload);

        // getMatch(), getMatch().getMetadata()
        MetadataBuilder metadataBuilder = new MetadataBuilder();
        metadataBuilder.setMetadata(new BigInteger("100"));
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setMetadata(metadataBuilder.build());
        when(pktMock.getMatch()).thenReturn(matchBuilder.build());

        // getIngress()
        //    .getValue()
        //    .firstKeyOf(Node.class, NodeKey.class)
        //    .getId().getValue();
        InstanceIdentifier<Node> nodeId =
                InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .build();

        NodeConnectorRef nodeConnRef = new NodeConnectorRef(nodeId);
        when(pktMock.getIngress()).thenReturn(nodeConnRef);

        return pktMock;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
