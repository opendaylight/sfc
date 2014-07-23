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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.PutServiceFunctionForwarderInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.PutServiceFunctionForwarderInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarderService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.entry.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.entry.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.PutServiceNodeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodeService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.binding.DataContainer;
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
 * running JVM processes, select "org.eclipse.equinox.launcher.Main –console –consoleLog"
 * (this is how it looks on Windows, maybe on Mac you will see some difference), or
 * type in "remote connections" localhost:1088
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
    private final ServiceNodeService snService;
    private final ServiceFunctionForwarderService sffService;

    public SfcTestConsumerImpl(
            ServiceFunctionService sfService,
            ServiceFunctionChainService sfcService,
            ServiceNodeService snService,
            ServiceFunctionForwarderService sffService) {
        this.sfService = sfService;
        this.sfcService = sfcService;
        this.snService = snService;
        this.sffService = sffService;
    }

    /**
     * Local method used by JMX RPCs. This is the method that actually
     * sends RPC request to SFC Provider to create Service Functions
     *
     * @return Boolean
     */
    private Boolean putSf(String name, String type,
                          String ipMgmt, String ipLocator, int portLocator, String sffName) {
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
        putServiceFunctionInputBuilder
                .setName(name)
                .setType(type)
                .setIpMgmtAddress(new IpAddress(ipMgmt.toCharArray()))
                .setSfDataPlaneLocator(sfDataPlaneLocatorBuilder.build())
                .setServiceFunctionForwarder(sffName);

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

    private Boolean putNode(String name,
                            String ip,
                            List<String> sfList) {

        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        PutServiceNodeInputBuilder input = new PutServiceNodeInputBuilder();

        input.setName(name)
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(ip)))
                .setServiceFunction(sfList);
        try {
            Future<RpcResult<Void>> fr = snService.putServiceNode(input.build());
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
     * Puts an SFChain
     * @param name Name for new service function chain
     * @param sfList List of references to service functions (by names)
     * @return Boolean
     */
    private Boolean putChain(String name, List<SfcServiceFunction> sfList) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        PutServiceFunctionChainsInputBuilder input = new PutServiceFunctionChainsInputBuilder();
        ServiceFunctionChainBuilder sfcBuilder = new ServiceFunctionChainBuilder();

        sfcBuilder.setName(name).setSfcServiceFunction(sfList);
        List<ServiceFunctionChain> list = new ArrayList<>();
        list.add(sfcBuilder.build());

        input.setServiceFunctionChain(list);

        try {
            Future<RpcResult<Void>> fr = sfcService.putServiceFunctionChains(input.build());
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

    private Boolean putForwarder(String name,
                            String ip,
                            Integer port) {

        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        PutServiceFunctionForwarderInputBuilder input = new PutServiceFunctionForwarderInputBuilder();

        SffDataPlaneLocatorBuilder locatorBuilder = new SffDataPlaneLocatorBuilder();
        IpBuilder ipBuilder = new IpBuilder();
        locatorBuilder.setLocatorType(ipBuilder.setIp(new IpAddress(new Ipv4Address(ip)))
                .setPort(new PortNumber(port)).build());
        input.setName(name)
                .setServiceFunctionDictionary(new ArrayList<ServiceFunctionDictionary>())
                .setSffDataPlaneLocator(locatorBuilder.build())
                .setTransport(VxlanGpe.class);
        try {
            Future<RpcResult<Void>> fr = sffService.putServiceFunctionForwarder(input.build());
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
    public Boolean testAPutSf() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        return putSf("firewall-test", "firewall", "10.0.0.2", "192.168.0.2", 5050, "SFF-testA");
    }

    /**
     * Function for JMX testing.
     * Reads a sfService function by fixed name.
     *
     * @return Boolean
     */
    @Override
    public Boolean testAReadSf() {
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
    public Boolean testADeleteSf() {
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

        Boolean res = putSf("firewall-testB", "firewall", "10.0.0.101", "192.168.0.101", 5050, "SFF-testB");
        res = putSf("dpi-testB", "dpi", "10.0.0.102", "192.168.0.102", 5050, "SFF-testB") && res;
        res = putSf("napt44-testB", "napt44", "10.0.0.103", "192.168.0.102", 5050, "SFF-testB") && res;

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

    @Override
    public Boolean testCPutData() {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        Boolean res = putForwarder("SFF-testC", "10.3.1.101", 33333);

        // Service Functions (real, not abstract)
        res = putSf("firewall-101-1", "firewall", "10.3.1.101", "10.3.1.101", 10001, "SFF-testC") && res;
        res = putSf("firewall-101-2", "firewall", "10.3.1.101", "10.3.1.101", 10002, "SFF-testC") && res;
        res = putSf("dpi-102-1", "dpi", "10.3.1.102", "10.3.1.102", 10001, "SFF-testC") && res;
        res = putSf("dpi-102-2", "dpi", "10.3.1.102", "10.3.1.102", 10002, "SFF-testC") && res;
        res = putSf("dpi-102-3", "dpi", "10.3.1.102", "10.3.1.102", 10003, "SFF-testC") && res;
        res = putSf("napt44-103-1", "napt44", "10.3.1.103", "10.3.1.103", 10001, "SFF-testC") && res;
        res = putSf("napt44-103-2", "napt44", "10.3.1.103", "10.3.1.103", 10002, "SFF-testC") && res;
        res = putSf("firewall-104", "firewall", "10.3.1.104", "10.3.1.104", 10001, "SFF-testC") && res;
        res = putSf("napt44-104", "napt44", "10.3.1.104", "10.3.1.104", 10020, "SFF-testC") && res;

        // SFC1
        List<SfcServiceFunction> sfRefList = new ArrayList<>();
        SfcServiceFunctionBuilder sfBuilder = new SfcServiceFunctionBuilder();
        sfRefList.add(sfBuilder.setName("firewall-abstract1").setType("firewall").build());
        sfRefList.add(sfBuilder.setName("dpi-abstract1").setType("dpi").build());
        sfRefList.add(sfBuilder.setName("napt44-abstract1").setType("napt44").build());

        res = putChain("SFC1", sfRefList) && res;

        // SFC2
        sfRefList.clear();
        sfBuilder = new SfcServiceFunctionBuilder();
        sfRefList.add(sfBuilder.setName("firewall-abstract2").setType("firewall").build());
        sfRefList.add(sfBuilder.setName("napt44-abstract2").setType("napt44").build());

        res = putChain("SFC2", sfRefList) && res;

        // Nodes
        List<String> iList = new ArrayList<>();
        iList.add("firewall-101-1");
        iList.add("firewall-101-2");

        res = putNode("node-101", "10.3.1.101", iList) && res;

        iList.clear();
        iList.add("dpi-102-1");
        iList.add("dpi-102-2");
        iList.add("dpi-102-3");

        res = putNode("node-102", "10.3.1.102", iList) && res;

        iList.clear();
        iList.add("napt44-103-1");
        iList.add("napt44-103-2");

        res = putNode("node-103", "10.3.1.103", iList) && res;

        iList.clear();
        iList.add("firewall-104");
        iList.add("napt44-104");

        res = putNode("node-104", "10.3.1.104", iList) && res;

        return res;
    }
}
