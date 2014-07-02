/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140629.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.PutServiceFunctionChainsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.service.function.chain.grouping.service.function.chain.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a harness for live unit Testing. It has static
 * methods that can are used to test various SFC datastore operations.
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov ()
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderUnitTest {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderUnitTest.class);
    // Delete all Service Functions
    public static void sfcProviderUnitTest(SfcProviderRpc sfcRpcObj) {
        sfcRpcObj.deleteAllServiceFunction();
        // Put one Service Function
        PutServiceFunctionInputBuilder psfibuilder = new PutServiceFunctionInputBuilder();
        IpAddress ip = new IpAddress("10.0.0.1".toCharArray());
        PutServiceFunctionInput psfi = psfibuilder.setName("test-fw-ca").setType(Firewall.class).setIpMgmtAddress(ip).build();
        sfcRpcObj.putServiceFunction(psfi);
        // Put one Service Function
        psfibuilder = new PutServiceFunctionInputBuilder();
        ip = new IpAddress("10.0.0.2".toCharArray());
        psfi = psfibuilder.setName("test-fw-ut").setType(Firewall.class).setIpMgmtAddress(ip).build();
        sfcRpcObj.putServiceFunction(psfi);
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
        List<ServiceFunctionType> sftlist = new ArrayList<>();

        ServiceFunctionTypeBuilder sftBuilder = new ServiceFunctionTypeBuilder();
        sftlist.add(sftBuilder.setName("napt44-testB").setType("napt44").build());
        sftlist.add(sftBuilder.setName("firewall-testB").setType("firewall").build());
        sftlist.add(sftBuilder.setName("dpi-testB").setType("dpi").build());

        // Now we add list function type list to Service Chain list.
        sfclist.add(sfcbuilder.setName("Chain-1").setServiceFunctionType(sftlist).build());

        psfcibuilder = psfcibuilder.setServiceFunctionChain(sfclist);
        sfcRpcObj.putServiceFunctionChains(psfcibuilder.build());

    }
}
