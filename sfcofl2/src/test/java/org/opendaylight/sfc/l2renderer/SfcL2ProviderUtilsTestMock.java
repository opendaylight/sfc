package org.opendaylight.sfc.l2renderer;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2ProviderUtilsTestMock extends SfcL2AbstractProviderUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2ProviderUtilsTestMock.class);
    private Map<String, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
    private Map<String, ServiceFunctionForwarder> serviceFunctionForwarders;

    public SfcL2ProviderUtilsTestMock() {
        LOG.info("SfcL2ProviderUtilsTestMock constructor");
        serviceFunctions = new HashMap<String, ServiceFunction>();
        serviceFunctionGroups = new HashMap<String, ServiceFunctionGroup>();
        serviceFunctionForwarders = new HashMap<String, ServiceFunctionForwarder>();
    }

    public void addServiceFunction(String sfName, ServiceFunction sf) {
        serviceFunctions.put(sfName, sf);
    }

    public void addServiceFunctionForwarder(String sffName, ServiceFunctionForwarder sff) {
        serviceFunctionForwarders.put(sffName, sff);
    }

    public void addServiceFunctionGroup(String sfgName, ServiceFunctionGroup sfg) {
        serviceFunctionGroups.put(sfgName, sfg);
    }

    // Only needed for multi-threading, empty for now
    public void addRsp(long rspId) {
    }

    // Only needed for multi-threading, empty for now
    public void removeRsp(long rspId) {
    }

    public void resetCache() {
        LOG.info("SfcL2ProviderUtilsTestMock resetCache");
        serviceFunctions.clear();
        serviceFunctionGroups.clear();
        serviceFunctionForwarders.clear();
    }

    public ServiceFunction getServiceFunction(String sfName, long pathId) {
        return serviceFunctions.get(sfName);
    }

    public ServiceFunctionForwarder getServiceFunctionForwarder(String sffName, long pathId) {
        return serviceFunctionForwarders.get(sffName);
    }

    public ServiceFunctionGroup getServiceFunctionGroup(String sfgName, long pathId) {
        return serviceFunctionGroups.get(sfgName);
    }

}
