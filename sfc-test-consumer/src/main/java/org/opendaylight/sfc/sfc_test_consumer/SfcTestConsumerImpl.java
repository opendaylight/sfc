/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_test_consumer;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;

import org.opendaylight.controller.config.yang.config.sfc_test_consumer.impl.SfcTestConsumerRuntimeMXBean;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Manually coded consumer implementation
 * according to example at https://wiki.opendaylight.org/view/OpenDaylight_Controller:
 *    MD-SAL:Toaster_Step-By-Step#Define_the_KitchenService_implementation
 *
 * How to Exercise these RPCs:
 *
 * In order to to exercise these RPCs controller has to be started as: ./run –jmx
 *
 * Then start jconsole from /bin inside your $JAVA_HOME. It will show a list of
 * running JVM processes, select “org.eclipse.equinox.launcher.Main –console –consoleLog”
 * (this is how it looks on Windows, maybe on Mac you will see some difference), or
 * type in “remote connections” localhost:1088
 *
 * Allow insecure connection if it asks. There will be the rightmost tab called
 * MBeans, with a folded tree of different components.
 * Go to org.opendaylight.controller -> Runtime bean -> test-consumer-impl (some times it
 * takes a little time until all those beans are initialized, they appear one by one),
 * then once again test-consumer-impl (on next level) -> Operations, and there they
 * are, all RPC from consumer YANG, mapped to their Java implementations. When you
 * select one, on the right panel there is a button to invoke that function.
 *
 * <p>
 * @author Konstantin Blagov ()
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-07-01
 */


public class SfcTestConsumerImpl implements SfcTestConsumer, SfcTestConsumerRuntimeMXBean {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcTestConsumerImpl.class);

    private final ServiceFunctionService sfService;
    private final ServiceFunctionChainService sfcService;

    public SfcTestConsumerImpl(
            ServiceFunctionService sfService,
            ServiceFunctionChainService sfcService) {
        this.sfService = sfService;
        this.sfcService = sfcService;
    }

    /**
     * Local method used by JMX RPCs. This is the method that actually
     * sends RPC request to SFC Provider to create Service Functions
     *
     * @return Boolean
     */
    private Boolean putSf(String name, String type,
                          String ipMgmt, String ipLocator, int portLocator) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        // Build Locator Type (ip and port)
        IpAddress ipAddress = new IpAddress(ipLocator.toCharArray());
        PortNumber portNumber = new PortNumber(portLocator);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder = ipBuilder.setIp(ipAddress).setPort(portNumber);



        // Build Data Plane Locator and populate with Locator Type

        SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
        sfDataPlaneLocatorBuilder = sfDataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());

        // Build ServiceFunctionBuilder and set all data constructed above
        PutServiceFunctionInputBuilder putServiceFunctionInputBuilder = new PutServiceFunctionInputBuilder();
        putServiceFunctionInputBuilder = putServiceFunctionInputBuilder.setName(name).setType(type).
                setIpMgmtAddress(new IpAddress(ipMgmt.toCharArray())).
                setSfDataPlaneLocator(sfDataPlaneLocatorBuilder.build());

        try {
            Future<RpcResult<Void>> fr = sfService.putServiceFunction(putServiceFunctionInputBuilder.build());
            RpcResult<Void> result = fr.get();
            if (result != null) {
                LOG.info("\n####### {} result: {}", Thread.currentThread().getStackTrace()[1], result);
                if (result.isSuccessful()) {
                    LOG.info("\n####### {}: successfully finished", Thread.currentThread().getStackTrace()[1]);
                } else {
                    LOG.warn("\n####### {}: not successfully finished", Thread.currentThread().getStackTrace()[1]);
                }
                return result.isSuccessful();
            } else {
                LOG.warn("\n####### {} result is NULL", Thread.currentThread().getStackTrace()[1]);
                return Boolean.FALSE;
            }

        } catch (Exception e) {
            LOG.warn("\n####### {} Error occurred: {}", Thread.currentThread().getStackTrace()[1], e);
            e.printStackTrace();
            return Boolean.FALSE;
        }
    }


    /**
     * Function for JMX testing.
     * Creates a new sfService function with fixed parameters.
     *
     * @return Boolean
     */
    @Override
    public Boolean testPutSf() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        return putSf("firewall-test", "firewall", "10.0.0.2", "192.168.0.2", 5050);
    }

    /**
     * Function for JMX testing.
     * Reads a sfService function by fixed name.
     *
     * @return Boolean
     */
    @Override
    public Boolean testReadSf() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ReadServiceFunctionInputBuilder input = new ReadServiceFunctionInputBuilder();
        input.setName("firewall-test");

        try {
            RpcResult<ReadServiceFunctionOutput> result = sfService.readServiceFunction(input.build()).get();
            if (result.isSuccessful()) {
                LOG.info("testReadSf: successfully finished, result: ", result.getResult());
            } else {
                LOG.warn("testReadSf: not successfully finished");
            }
            return result.isSuccessful();
        } catch (Exception e) {
            LOG.warn("Error occurred during testReadSf: " + e);
        }
        return false;
    }

    /**
     * Function for JMX testing.
     * Deletes a sfService function by fixed name.
     *
     * @return Boolean
     */
    @Override
    public Boolean testDeleteSf() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        DeleteServiceFunctionInputBuilder input = new DeleteServiceFunctionInputBuilder();
        input.setName("firewall-test");

        try {
            Future<RpcResult<Void>> fr = sfService.deleteServiceFunction(input.build());
            RpcResult<Void> result = fr.get();
            if (result != null) {
                if (result.isSuccessful()) {
                    LOG.info("testDeleteSf: successfully finished");
                } else {
                    LOG.warn("testDeleteSf: not successfully finished");
                }
                return result.isSuccessful();
            } else {
                LOG.warn("*****\ntestDeleteSf result is NULL");
            }
        } catch (Exception e) {
            LOG.warn("Error occurred during testDeleteSf: " + e);
            e.printStackTrace();
        }
        return Boolean.FALSE;
    }

    /**
     * Function for JMX testing.
     * Creates multiple Service Functions with fixed parameters.
     *
     * @return Boolean
     */
    @Override
    public Boolean testBPutSfs() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        Boolean res = putSf("firewall-testB", "firewall", "10.0.0.101", "192.168.0.101", 5050);
        res = putSf("dpi-testB", "dpi", "10.0.0.102", "192.168.0.102", 5050) && res;
        res = putSf("napt44-testB", "napt44", "10.0.0.103", "192.168.0.102", 5050) && res;

        return res;
    }


    /**
     * Function for JMX testing.
     * Creates a Service Chain with fixed parameters.
     *
     * @return Boolean
     */
    @Override
    public Boolean testBPutSfc() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        //Put a service chain. We need to build a list of lists.
        PutServiceFunctionChainsInputBuilder putServiceFunctionChainsInputBuilder = new PutServiceFunctionChainsInputBuilder();
        ServiceFunctionChainBuilder sfcbuilder = new ServiceFunctionChainBuilder();

        List<ServiceFunctionChain> sfclist = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionArrayList = new ArrayList<>();

        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionArrayList.add(sfcServiceFunctionBuilder.setName("napt44-testB").setType("napt44").build());
        sfcServiceFunctionArrayList.add(sfcServiceFunctionBuilder.setName("firewall-testB").setType("firewall").build());
        sfcServiceFunctionArrayList.add(sfcServiceFunctionBuilder.setName("dpi-testB").setType("dpi").build());

        // Now we add list function type list to Service Chain list.
        sfclist.add(sfcbuilder.setName("Chain-1").setSfcServiceFunction(sfcServiceFunctionArrayList).build());

        putServiceFunctionChainsInputBuilder = putServiceFunctionChainsInputBuilder.setServiceFunctionChain(sfclist);

        try {
            Future<RpcResult<Void>> fr = sfcService.
                    putServiceFunctionChains(putServiceFunctionChainsInputBuilder.build());
            RpcResult<Void> result = fr.get();
            if (result != null) {
                LOG.info("\n####### {} result: {}", Thread.currentThread().getStackTrace()[1], result);
                if (result.isSuccessful()) {
                    LOG.info("\n####### {}: successfully finished", Thread.currentThread().getStackTrace()[1]);
                } else {
                    LOG.warn("\n####### {}: not successfully finished", Thread.currentThread().getStackTrace()[1]);
                }
                return result.isSuccessful();
            } else {
                LOG.warn("\n####### {} result is NULL", Thread.currentThread().getStackTrace()[1]);
                return Boolean.FALSE;
            }

        } catch (Exception e) {
            LOG.warn("\n####### {} Error occurred: {}", Thread.currentThread().getStackTrace()[1], e);
            e.printStackTrace();
            return Boolean.FALSE;
        }

    }

    @Override
    public Boolean testBReadSfc() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        return null;
    }

    @Override
    public Boolean testBDeleteSfc() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        return null;
    }
}
