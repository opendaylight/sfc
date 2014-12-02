/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.PutServiceFunctionChainsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Dpi;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Firewall;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.Napt44;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.
        data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.
        data.plane.locator.data.plane.locator.locator.type.IpBuilder;
*/

/**
 * This class is a harness for live unit Testing. It has static
 * methods that can are used to test various SFC datastore operations.
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderUnitTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderUnitTest.class);

    private SfcProviderUnitTest() {}

    // Delete all Service Functions
    public static void sfcProviderUnitTest(SfcProviderRpc sfcRpcObj) {
        sfcRpcObj.deleteAllServiceFunction();

        // Put one Service Function
/*
        // Build Locator Type (ip and port)
        IpAddress ipaddr = new IpAddress("10.0.0.1".toCharArray());
        IpBuilder ipBuilder = new IpBuilder();
        PortNumber portNumber = new PortNumber(5050);
        ipBuilder = ipBuilder.setPort(portNumber).setIp(ipaddr);

        // Build Data Plane Locator and populate with Locator Type
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder = dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());

        // Build ServiceFunctionBuilder and set all data constructed above
        PutServiceFunctionInputBuilder putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName("test-fw-ca").setType("Firewall.class").
                setIpMgmtAddress(new IpAddress("192.168.0.1".toCharArray())).
                setDataPlaneLocator(dataPlaneLocatorBuilder.build());

        PutServiceFunctionInput psfi = putServiceFunctionInputBuilder.build();
        sfcRpcObj.putServiceFunction(psfi);

        // Put one Service Function

        // Build Locator Type (ip and port)
        ipaddr = new IpAddress("10.0.0.2".toCharArray());
        ipBuilder = new IpBuilder();
        portNumber = new PortNumber(5050);
        ipBuilder = ipBuilder.setPort(portNumber).setIp(ipaddr);

        // Build Data Plane Locator and populate with Locator Type
        dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder = dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());

        // Build ServiceFunctionBuilder and set all data constructed above
        putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName("test-dpi-ut").setType("Dpi.class").
                setIpMgmtAddress(new IpAddress("192.168.0.2".toCharArray())).
                setDataPlaneLocator(dataPlaneLocatorBuilder.build());

        // Put one Service Function
        psfi = putServiceFunctionInputBuilder.build();
        sfcRpcObj.putServiceFunction(psfi);

*/
        // Delete one Service Function
        DeleteServiceFunctionInputBuilder dsfibuilder = new DeleteServiceFunctionInputBuilder();
        DeleteServiceFunctionInput dsfi = dsfibuilder.setName("test-fw-ca").build();
        sfcRpcObj.deleteServiceFunction(dsfi);

        // Read One Service Function
        ReadServiceFunctionInputBuilder rsfibuilder = new ReadServiceFunctionInputBuilder();
        ReadServiceFunctionInput rsfi = rsfibuilder.setName("test-fw-ut").build();
        try {
            RpcResult<ReadServiceFunctionOutput> result = sfcRpcObj.readServiceFunction(rsfi).get();
            if (result.isSuccessful()) {
                ReadServiceFunctionOutput rsfo = result.getResult();
                LOG.info("\n########## Read Service Function: {}", rsfo.getType());
            } else {
                LOG.warn("Read Service Function RPC not successfully finished");
            }
        } catch (Exception e) {
            LOG.warn("Error occurred during SF Read Test: " + e);
        }

        //Put a service chain. We need to build a list of lists.
        PutServiceFunctionChainsInputBuilder psfcibuilder = new PutServiceFunctionChainsInputBuilder();
        ServiceFunctionChainBuilder sfcbuilder = new ServiceFunctionChainBuilder();

        List<ServiceFunctionChain> sfclist = new ArrayList<>();
        List<SfcServiceFunction> sftlist = new ArrayList<>();

        SfcServiceFunctionBuilder sftBuilder = new SfcServiceFunctionBuilder();
        sftlist.add(sftBuilder.setName("napt44-testB").setType(Napt44.class).build());
        sftlist.add(sftBuilder.setName("firewall-testB").setType(Firewall.class).build());
        sftlist.add(sftBuilder.setName("dpi-testB").setType(Dpi.class).build());

        // Now we add list function type list to Service Chain list.
        sfclist.add(sfcbuilder.setName("Chain-1").setSfcServiceFunction(sftlist).build());

        psfcibuilder = psfcibuilder.setServiceFunctionChain(sfclist);
        sfcRpcObj.putServiceFunctionChains(psfcibuilder.build());

    }
}
