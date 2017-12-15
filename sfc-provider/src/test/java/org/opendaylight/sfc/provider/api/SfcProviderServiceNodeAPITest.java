/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

/**
 * Unit tests for the @{@link SfcProviderServiceNodeAPI} class.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
public class SfcProviderServiceNodeAPITest extends AbstractConcurrentDataBrokerTest {

    private static final String SERVICE_NODE_NAME = "ServiceNode1";
    private static final String SERVICE_NODE_IP_MANAGEMENT_ADDRESS = "196.168.55.101";

    private ServiceNode serviceNode;

    @Before
    public void before() {
        SfcDataStoreAPI.setDataProviderAux(getDataBroker());
        ServiceNodeBuilder serviceNodeBuilder = new ServiceNodeBuilder();
        serviceNodeBuilder.setKey(new ServiceNodeKey(new SnName(SERVICE_NODE_NAME)))
                .setName(new SnName(SERVICE_NODE_NAME))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(SERVICE_NODE_IP_MANAGEMENT_ADDRESS)));
        this.serviceNode = serviceNodeBuilder.build();
    }

    @Test
    public void testCreateReadServiceNode() {
        SfcProviderServiceNodeAPI.putServiceNode(serviceNode);
        serviceNode = SfcProviderServiceNodeAPI.readServiceNodeByName(new SnName(SERVICE_NODE_NAME));
        assertNotNull("Must not be null", serviceNode);
        assertEquals("Must be equals", serviceNode.getName().getValue(), SERVICE_NODE_NAME);
        assertEquals("Must be equals", serviceNode.getIpMgmtAddress().getIpv4Address().getValue(),
                     SERVICE_NODE_IP_MANAGEMENT_ADDRESS);
    }

    @Test
    public void testDeleteServiceNode() {
        SfcProviderServiceNodeAPI.putServiceNode(serviceNode);
        boolean result = SfcProviderServiceNodeAPI.deleteServiceNodeByName(new SnName(SERVICE_NODE_NAME));
        assertTrue(result);
        assertNull(SfcProviderServiceNodeAPI.readServiceNodeByName(new SnName(SERVICE_NODE_NAME)));
    }

    @Test
    public void testReadAllServiceNodes() {
        SfcProviderServiceNodeAPI.putServiceNode(serviceNode);
        ServiceNodes serviceNodes = SfcProviderServiceNodeAPI.readAllServiceNodes();
        assertNotNull("Must not be null", serviceNodes);
        assertEquals("Must be equals", serviceNodes.getServiceNode().get(0).getName().getValue(), SERVICE_NODE_NAME);
        assertEquals("Must be equals",
                     serviceNodes.getServiceNode().get(0).getIpMgmtAddress().getIpv4Address().getValue(),
                     SERVICE_NODE_IP_MANAGEMENT_ADDRESS);
    }
}
