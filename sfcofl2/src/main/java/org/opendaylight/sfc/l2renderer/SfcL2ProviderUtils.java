package org.opendaylight.sfc.l2renderer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;

public class SfcL2ProviderUtils extends SfcL2BaseProviderUtils {

    // Since this class can be called by multiple threads,
    // store these objects per RSP id to avoid collisions
    private class RspContext {
        // store the SFs and SFFs internally so we dont have to
        // query the DataStore repeatedly for the same thing
        private Map<String, ServiceFunction> serviceFunctions;
        private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
        private Map<String, ServiceFunctionForwarder> serviceFunctionFowarders;

        public RspContext() {
            serviceFunctions = Collections.synchronizedMap(new HashMap<String, ServiceFunction>());
            serviceFunctionGroups = Collections.synchronizedMap(new HashMap<String, ServiceFunctionGroup>());
            serviceFunctionFowarders = Collections.synchronizedMap(new HashMap<String, ServiceFunctionForwarder>());
        }
    }
    private Map<Long, RspContext> rspIdToContext;

    public SfcL2ProviderUtils() {
        rspIdToContext = new HashMap<Long, RspContext>();
    }

    public void addRsp(long rspId) {
        rspIdToContext.put(rspId, new RspContext());
    }

    public void removeRsp(long rspId) {
        rspIdToContext.remove(rspId);
    }

    /**
     * Return the named ServiceFunction
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     *
     * @param sfName - The SF Name to search for
     * @return - The ServiceFunction object, or null if not found
     */
    public ServiceFunction getServiceFunction(final String sfName, long rspId) {
        RspContext rspContext = rspIdToContext.get(rspId);

        ServiceFunction sf = rspContext.serviceFunctions.get(sfName);
        if(sf == null) {
            sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfName);
            if(sf != null) {
                rspContext.serviceFunctions.put(sfName, sf);
            }
        }

        return sf;
    }

    /**
     * Return the named ServiceFunctionForwarder
     * Acts as a local cache to not have to go to DataStore so often
     * First look in internal storage, if its not there
     * get it from the DataStore and store it internally
     *
     * @param sffName - The SFF Name to search for
     * @return The ServiceFunctionForwarder object, or null if not found
     */
    public ServiceFunctionForwarder getServiceFunctionForwarder(final String sffName, long rspId) {
        RspContext rspContext = rspIdToContext.get(rspId);

        ServiceFunctionForwarder sff = rspContext.serviceFunctionFowarders.get(sffName);
        if(sff == null) {
            sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sffName);
            if(sff != null) {
                rspContext.serviceFunctionFowarders.put(sffName, sff);
            }
        }

        return sff;
    }

    public ServiceFunctionGroup getServiceFunctionGroup(final String sfgName, long rspId) {
        RspContext rspContext = rspIdToContext.get(rspId);

        ServiceFunctionGroup sfg = rspContext.serviceFunctionGroups.get(sfgName);
        if (sfg == null) {
            sfg = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroupExecutor(sfgName);
            if (sfg != null) {
                rspContext.serviceFunctionGroups.put(sfgName, sfg);
            }
        }

        return sfg;
    }

}
