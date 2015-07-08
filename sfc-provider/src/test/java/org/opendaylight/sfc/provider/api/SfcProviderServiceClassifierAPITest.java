package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Before;
import org.junit.Test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiersState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.ServiceFunctionClassifierState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.ServiceFunctionClassifierStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.state.service.function.classifier.state.SclRenderedServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings({ "rawtypes" })
public class SfcProviderServiceClassifierAPITest extends AbstractDataBrokerTest {

    private OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    private static boolean initialized = false;

    @Before
    public void setUp() throws Exception {
        if (!initialized) {
            DataBroker dataBroker = getDataBroker();
            opendaylightSfc.setDataProvider(dataBroker);
            initialized = true;
        }
    }

    @Test
    public void addRenderedPathToServiceClassifierStateTest() throws Exception {
        final String serviceClassifierName = "serviceClassifierName";
        final String renderedServicePath = "renderedServicePathName";
        final String instanceIdInfo = serviceClassifierName + "/" + renderedServicePath;

        SclRenderedServicePathBuilder sclRenderedServicePathBuilder = new SclRenderedServicePathBuilder();
        SclRenderedServicePathKey sclRenderedServicePathKey = new SclRenderedServicePathKey(renderedServicePath);
        sclRenderedServicePathBuilder.setKey(sclRenderedServicePathKey).setName(renderedServicePath);

        ServiceFunctionClassifierStateKey serviceFunctionClassifierStateKey = new ServiceFunctionClassifierStateKey(serviceClassifierName);

        InstanceIdentifier<SclRenderedServicePath> instanceId = InstanceIdentifier.builder(ServiceFunctionClassifiersState.class)
                .child(ServiceFunctionClassifierState.class, serviceFunctionClassifierStateKey)
                .child(SclRenderedServicePath.class, sclRenderedServicePathKey).build();

        assertNull("Unexpected object '" + instanceIdInfo + "' found in data store.",
                SfcDataStoreAPI.readTransactionAPI(instanceId, LogicalDatastoreType.OPERATIONAL));

        assertTrue("Failed to add object '" + instanceIdInfo + "' to data store.",
                SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierState(serviceClassifierName, renderedServicePath));

        SclRenderedServicePath sclRenderedServicePath = SfcDataStoreAPI.readTransactionAPI(instanceId, LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Object '" + instanceIdInfo + "' not found in data store.", sclRenderedServicePath);
        assertEquals("Unexpected object name.", renderedServicePath, sclRenderedServicePath.getName());
    }

    @Test
    public void addRenderedPathToServiceClassifierStateExecutorTest() throws Exception {
        final String serviceClassifierName = "_serviceClassifierName";
        final String renderedServicePathName = "renderedServicePathName";
        final String instanceIdInfo = serviceClassifierName + "/" + renderedServicePathName;

        SclRenderedServicePathBuilder sclRenderedServicePathBuilder = new SclRenderedServicePathBuilder();
        SclRenderedServicePathKey sclRenderedServicePathKey = new SclRenderedServicePathKey(renderedServicePathName);
        sclRenderedServicePathBuilder.setKey(sclRenderedServicePathKey).setName(renderedServicePathName);

        ServiceFunctionClassifierStateKey serviceFunctionClassifierStateKey = new ServiceFunctionClassifierStateKey(serviceClassifierName);

        InstanceIdentifier<SclRenderedServicePath> instanceId = InstanceIdentifier.builder(ServiceFunctionClassifiersState.class)
                .child(ServiceFunctionClassifierState.class, serviceFunctionClassifierStateKey)
                .child(SclRenderedServicePath.class, sclRenderedServicePathKey).build();

        assertNull("Unexpected object '" + instanceIdInfo + "' found in data store.",
                SfcDataStoreAPI.readTransactionAPI(instanceId, LogicalDatastoreType.OPERATIONAL));

        assertTrue("Failed to add object '" + instanceIdInfo + "' to data store.",
                SfcProviderServiceClassifierAPI.addRenderedPathToServiceClassifierStateExecutor(serviceClassifierName, renderedServicePathName));

        SclRenderedServicePath sclRenderedServicePath = SfcDataStoreAPI.readTransactionAPI(instanceId, LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Object '" + instanceIdInfo + "' not found in data store.", sclRenderedServicePath);
        assertEquals("Unexpected object name.", renderedServicePathName, sclRenderedServicePath.getName());
    }

    @Test
    public void readServiceClassifierExecutorTest() throws Exception {
        final String serviceClassifierName = "serviceClassifierName";

        assertNull("Unexpected classifier '" + serviceClassifierName + "' found in data store",
                SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(serviceClassifierName));

        InstanceIdentifier<ServiceFunctionClassifier> instanceId = InstanceIdentifier.builder(ServiceFunctionClassifiers.class)
                .child(ServiceFunctionClassifier.class, new ServiceFunctionClassifierKey(serviceClassifierName)).build();
        ServiceFunctionClassifierBuilder serviceFunctionClassifierBuilder = new ServiceFunctionClassifierBuilder();
        serviceFunctionClassifierBuilder.setName(serviceClassifierName);
        serviceFunctionClassifierBuilder.setKey(new ServiceFunctionClassifierKey(serviceClassifierName));

        assertTrue("Failed to write classifier to data store",
                SfcDataStoreAPI.writePutTransactionAPI(instanceId, serviceFunctionClassifierBuilder.build(), LogicalDatastoreType.CONFIGURATION));

        ServiceFunctionClassifier result = SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(serviceClassifierName);
        assertNotNull("Classifier '" + serviceClassifierName + "' not found in data store.", result);
        assertEquals(serviceClassifierName, result.getName());
    }

    @Test
    public void getReadTest() throws Exception {
        Object[] params = {"funcParam0", "funcParam1"};
        Class[] paramTypes = {String.class, String.class};
        SfcProviderServiceClassifierAPI api = SfcProviderServiceClassifierAPI.getRead(params, paramTypes);
        assertApiInitialization(api, params, paramTypes);
    }

    @Test
    public void getAddRenderedPathToServiceClassifierStateExecutorTest() throws Exception {
        Object[] params = {"funcParam0", "funcParam1"};
        Class[] paramTypes = {String.class, String.class};
        SfcProviderServiceClassifierAPI api = SfcProviderServiceClassifierAPI.getAddRenderedPathToServiceClassifierStateExecutor(params, paramTypes);
        assertApiInitialization(api, params, paramTypes);
    }

    private void assertApiInitialization(SfcProviderServiceClassifierAPI api,
            Object[] expectedParams, Class[] expectedParamTypes) {
        assertArrayEquals(expectedParams, api.getParameters());
        assertArrayEquals(expectedParamTypes, api.getParameterTypes());
        assertFalse(api.getMethodName().isEmpty());
    }
}
