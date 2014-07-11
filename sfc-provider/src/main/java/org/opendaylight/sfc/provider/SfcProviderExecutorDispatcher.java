package org.opendaylight.sfc.provider;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by repenno on 7/10/14.
 */
public class SfcProviderExecutorDispatcher implements Runnable {

    private ServiceFunctionChain serviceFunctionChain;
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    public enum OperationType {CREATE, DELETE}
    public static int numCreatedServicePath = 0;

    private OperationType operation = OperationType.CREATE;

    SfcProviderExecutorDispatcher (ServiceFunctionChain sfc, OperationType type) {
        this.serviceFunctionChain = sfc;
        this.operation = type;
    }

    public static  SfcProviderExecutorDispatcher getSfcProviderCreateProvisioningElement(ServiceFunctionChain sfc) {
        return new SfcProviderExecutorDispatcher(sfc, OperationType.CREATE);
    }


    public static  SfcProviderExecutorDispatcher getSfcProviderDeleteProvisioningElement(ServiceFunctionChain sfc) {
        return new SfcProviderExecutorDispatcher(sfc, OperationType.DELETE);
    }


    @Override
    public void run() {
        switch (operation) {
            case CREATE:
                createProvisioningElement(serviceFunctionChain);
                break;
            case DELETE:
                deleteProvisioningElement(serviceFunctionChain);
                break;
        }
    }

    public void createProvisioningElement (ServiceFunctionChain serviceFunctionChain) {
        odlSfc.executor.execute(SfcProviderServicePathAPI.getSfcProviderCreateServicePathAPI(serviceFunctionChain));
    }

    public void deleteProvisioningElement (ServiceFunctionChain serviceFunctionChain) {
        odlSfc.executor.execute(SfcProviderServicePathAPI.getSfcProviderCreateServicePathAPI(serviceFunctionChain));
    }
}
