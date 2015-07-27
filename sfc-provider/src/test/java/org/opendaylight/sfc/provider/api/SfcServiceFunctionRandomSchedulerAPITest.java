package org.opendaylight.sfc.provider.api;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.AbstractDataStoreManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This class contains unit tests for SfcServiceFunctionRandomSchedulerAPI
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-06-30
 */
public class SfcServiceFunctionRandomSchedulerAPITest extends AbstractDataStoreManager {

    private static final String SFC_NAME = "sfcName";
    private static final String SF_NAME = "sfName";
    private static final String SFP_NAME = "sfpName";
    private static final String SFF_NAME = "sffName";
    private static final String SFG_NAME = "sfgName";
    private SfcServiceFunctionRandomSchedulerAPI scheduler;

    @Before
    public void before() {
        setOdlSfc();
        scheduler = new SfcServiceFunctionRandomSchedulerAPI();
    }

    /*
     * returns service functions name list from service function chain
     */
    @Test
    public void testServiceFunctionRandomScheduler() {

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
     * from existing service functions and types, ans service function is found and returned as a string
     */
    @Test
    public void testServiceFunctionRandomScheduler1() {

        //create empty path
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

        //no types are written, should return null
        List<String> result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, serviceFunctionPathBuilder.build());

        assertNull("Must be null", result);

        //write types
        boolean transactionSuccessful = writeTypes(true);

        assertTrue("Must be true", transactionSuccessful);

        //no functions are written, should return empty array
        result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, serviceFunctionPathBuilder.build());

        assertTrue("Must be true", result.contains(null));

        //write functions of all types
        transactionSuccessful = writeServiceFunction("Firewall", true);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Dpi", true);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Qos", true);
        assertTrue("Must be true", transactionSuccessful);

        result = scheduler.scheduleServiceFunctions(createServiceFunctionChain(), 255, serviceFunctionPathBuilder.build());

        //expected list
        List<String> serviceFunctionTypes = new ArrayList<>();
        serviceFunctionTypes.add(SF_NAME + "Firewall");
        serviceFunctionTypes.add(SF_NAME + "Dpi");
        serviceFunctionTypes.add(SF_NAME + "Qos");

        assertNotNull("Must not be null", result);
        assertTrue("Must be true", result.containsAll(serviceFunctionTypes));

        //remove functions and types
        transactionSuccessful = writeServiceFunction("Firewall", false);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Dpi", false);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeServiceFunction("Qos", false);
        assertTrue("Must be true", transactionSuccessful);
        transactionSuccessful = writeTypes(false);
        assertTrue("Must be true", transactionSuccessful);
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
    private boolean writeTypes(boolean write) {
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

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sftIID, serviceFunctionTypesBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sftIID, LogicalDatastoreType.CONFIGURATION);
    }

    //write service function
    private boolean writeServiceFunction(String sfType, boolean write) {
        ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
        serviceFunctionBuilder.setName(SF_NAME + sfType)
                .setKey(new ServiceFunctionKey(SF_NAME + sfType))
                .setType(Firewall.class);
        InstanceIdentifier<ServiceFunction> sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(SF_NAME + sfType)).build();

        if (write)
            return SfcDataStoreAPI.writePutTransactionAPI(sfIID, serviceFunctionBuilder.build(), LogicalDatastoreType.CONFIGURATION);
        else
            return SfcDataStoreAPI.deleteTransactionAPI(sfIID, LogicalDatastoreType.CONFIGURATION);
    }
}