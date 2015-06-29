package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

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

    OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    @Before
    public void before() throws Exception {
        DataBroker dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
    }

    @After
    public void after() throws Exception {
        deleteAllObjectsFromDataStore();
    }

    private void deleteAllObjectsFromDataStore() throws Exception {
        ExecutorService executorService = opendaylightSfc.getExecutor();
        executorService.submit(SfcProviderServiceFunctionAPI.getDeleteAll(new Object[] {}, new Class[] {}));
        executorService.submit(SfcProviderServiceForwarderAPI.getDeleteAll(new Object[] {}, new Class[] {}));
        executorService.submit(SfcProviderServiceTypeAPI.getDeleteAll(new Object[] {}, new Class[] {}));
        executorService.submit(SfcProviderServiceChainAPI.getDeleteAll(new Object[] {}, new Class[] {}));
        executorService.submit(SfcProviderServicePathAPI.getDeleteAll(new Object[] {}, new Class[] {}));
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
        assertArrayEquals(params, api.getParameters());
        assertArrayEquals(paramTypes, api.getParameterTypes());
        assertFalse(api.getMethodName().isEmpty());
    }

    @Test
    public void getAddRenderedPathToServiceClassifierStateExecutorTest() throws Exception {
        Object[] params = {"funcParam0", "funcParam1"};
        Class[] paramTypes = {String.class, String.class};
        SfcProviderServiceClassifierAPI api = SfcProviderServiceClassifierAPI.getAddRenderedPathToServiceClassifierStateExecutor(params, paramTypes);
        assertArrayEquals(params, api.getParameters());
        assertArrayEquals(paramTypes, api.getParameterTypes());
        assertFalse(api.getMethodName().isEmpty());
    }

    @Test
    public void readServiceClassifierTest() throws Exception {
        Object[] params = {"funcParam0", "funcParam1"};
        Class[] paramTypes = {String.class, String.class};
        SfcProviderServiceClassifierAPI api = SfcProviderServiceClassifierAPI.getRead(params, paramTypes);
        assertArrayEquals(params, api.getParameters());
        assertArrayEquals(paramTypes, api.getParameterTypes());
        assertFalse(api.getMethodName().isEmpty());
    }
}
