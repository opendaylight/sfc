package org.opendaylight.sfc.provider.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for SfcServiceFunctionRoundRobinSchedulerAPI
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-29
 */
public class SfcServiceFunctionRoundRobinSchedulerAPITest extends AbstractDataBrokerTest {

    private static final String SFC_NAME = "sfcName";
    private static final String SF_NAME = "sfName";
    private static final String SFP_NAME = "sfpName";
    private static final String SFF_NAME = "sffName";
    private static final String SFG_NAME = "sfgName";
    private final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    private ExecutorService executor;

    @Before
    public void before() throws InterruptedException, IllegalAccessException {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
        executor = opendaylightSfc.getExecutor();

        //clear data store
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        Thread.sleep(1000);

        //before test, private static variable mapCountRoundRobin has to be restored to original state
        Whitebox.getField(SfcServiceFunctionRoundRobinSchedulerAPI.class, "mapCountRoundRobin").set(HashMap.class, new HashMap<>());
    }

    @After
    public void after() {
        executor.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[]{}, new Class[]{}));
        executor.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[]{}, new Class[]{}));
    }

    /*
     * returns service functions name list from service function chain
     */
    @Test
    public void testServiceFunctionRoundRobinScheduler() {
        SfcServiceFunctionRoundRobinSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();

        List<String> result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, createServiceFunctionPath());

        //list of all service function names, the one returned by scheduleServiceFunction have to be the same
        List<String> serviceFunctions = new ArrayList<>();
        serviceFunctions.add(SF_NAME + 1);
        serviceFunctions.add(SF_NAME + 2);
        serviceFunctions.add(SF_NAME + 3);

        assertNotNull("Must be not null", result);
        assertTrue("Must be equal", result.containsAll(serviceFunctions));
    }

    /*
     * from existing service function types, and service function is found and returned as a string
     */
    @Test
    public void testServiceFunctionRoundRobinScheduler1() {
        SfcServiceFunctionRoundRobinSchedulerAPI scheduler = new SfcServiceFunctionRoundRobinSchedulerAPI();

        //create empty path
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        //no types are written, should return null
        List<String> result;// = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, serviceFunctionPathBuilder.build());

        //assertNull("Must be null", result);

        //write types
        boolean transactionSuccessful = writeTypes();

        assertTrue("Must be true", transactionSuccessful);

        result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, serviceFunctionPathBuilder.build());

        List<String> serviceFunctionTypes = new ArrayList<>();
        serviceFunctionTypes.add(SF_NAME + "Firewall");
        serviceFunctionTypes.add(SF_NAME + "Dpi");
        serviceFunctionTypes.add(SF_NAME + "Qos");

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result.containsAll(serviceFunctionTypes));
    }

    //create service function chain with three entries
    private ServiceFunctionChain createServiceFunctionChain() {
        ServiceFunctionChainBuilder serviceFunctionChainBuilder = new ServiceFunctionChainBuilder();
        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        sfcServiceFunctionBuilder.setName(SF_NAME + 1)
                .setKey(new SfcServiceFunctionKey(SF_NAME + 1))
                .setType(Firewall.class);
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME + 2)
                .setKey(new SfcServiceFunctionKey(SF_NAME + 2))
                .setType(Dpi.class);
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());
        sfcServiceFunctionBuilder = new SfcServiceFunctionBuilder();
        sfcServiceFunctionBuilder.setName(SF_NAME + 3)
                .setKey(new SfcServiceFunctionKey(SF_NAME + 3))
                .setType(Qos.class);
        sfcServiceFunctionList.add(sfcServiceFunctionBuilder.build());

        serviceFunctionChainBuilder.setName(SFC_NAME)
                .setKey(new ServiceFunctionChainKey(SFC_NAME))
                .setSymmetric(true)
                .setSfcServiceFunction(sfcServiceFunctionList);

        return serviceFunctionChainBuilder.build();
    }

    //create service function list
    private List<SftServiceFunctionName> createSftServiceFunctionNames(String serviceFunctionType) {
        List<SftServiceFunctionName> sftServiceFunctionNames = new ArrayList<>();

        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();

        sftServiceFunctionNameBuilder.setName(SF_NAME + serviceFunctionType)
                .setKey(new SftServiceFunctionNameKey(SF_NAME + serviceFunctionType));
        sftServiceFunctionNames.add(sftServiceFunctionNameBuilder.build());

        return sftServiceFunctionNames;
    }

    //create service function path
    private ServiceFunctionPath createServiceFunctionPath() {
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        List<ServicePathHop> servicePathHopList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();
            servicePathHopBuilder.setHopNumber((short) i)
                    .setKey(new ServicePathHopKey((short) i))
                    .setServiceFunctionForwarder(SFF_NAME)
                    .setServiceFunctionGroupName(SFG_NAME)
                    .setServiceIndex((short) (i + 1))
                    .setServiceFunctionName(SF_NAME + (i + 1));
            servicePathHopList.add(servicePathHopBuilder.build());
        }

        serviceFunctionPathBuilder.setName(SFP_NAME)
                .setKey(new ServiceFunctionPathKey(SFP_NAME))
                .setServicePathHop(servicePathHopList);

        return serviceFunctionPathBuilder.build();
    }

    //write types
    private boolean writeTypes() {
        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();
        List<ServiceFunctionType> serviceFunctionTypeList = new ArrayList<>();

        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setSftServiceFunctionName(createSftServiceFunctionNames("Firewall"))
                .setType(Firewall.class);
        serviceFunctionTypeList.add(serviceFunctionTypeBuilder.build());

        serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setSftServiceFunctionName(createSftServiceFunctionNames("Dpi"))
                .setType(Dpi.class);
        serviceFunctionTypeList.add(serviceFunctionTypeBuilder.build());

        serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder.setSftServiceFunctionName(createSftServiceFunctionNames("Qos"))
                .setType(Qos.class);
        serviceFunctionTypeList.add(serviceFunctionTypeBuilder.build());

        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeList);

        InstanceIdentifier<ServiceFunctionTypes> sftIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

        return SfcDataStoreAPI.writePutTransactionAPI(sftIID, serviceFunctionTypesBuilder.build(), LogicalDatastoreType.CONFIGURATION);
    }
}