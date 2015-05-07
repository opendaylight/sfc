package org.opendaylight.sfc.l2renderer;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2ProviderUtils extends SfcL2AbstractProviderUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2ProviderUtils.class);
    // each time onDataChanged() is called, store the SFs and SFFs internally
    // so we dont have to query the DataStore repeatedly for the same thing
    private Map<String, ServiceFunction> serviceFunctions;
    private Map<String, ServiceFunctionGroup> serviceFunctionGroups;
    private Map<String, ServiceFunctionForwarder> serviceFunctionFowarders;

    // This class cant be initialized
    public SfcL2ProviderUtils() {
        serviceFunctions = new HashMap<String, ServiceFunction>();
        serviceFunctionGroups = new HashMap<String, ServiceFunctionGroup>();
        serviceFunctionFowarders = new HashMap<String, ServiceFunctionForwarder>();
    }

    public void resetCache() {
        serviceFunctions.clear();
        serviceFunctionGroups.clear();
        serviceFunctionFowarders.clear();
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
    public ServiceFunction getServiceFunction(final String sfName) {
        ServiceFunction sf = serviceFunctions.get(sfName);
        if(sf == null) {
            sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfName);
            if(sf != null) {
                serviceFunctions.put(sfName, sf);
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
    public ServiceFunctionForwarder getServiceFunctionForwarder(final String sffName) {
        ServiceFunctionForwarder sff = serviceFunctionFowarders.get(sffName);
        if(sff == null) {
            sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sffName);
            if(sff != null) {
                serviceFunctionFowarders.put(sffName, sff);
            }
        }

        return sff;
    }

    public ServiceFunctionGroup getServiceFunctionGroup(final String sfgName) {
        ServiceFunctionGroup sfg = serviceFunctionGroups.get(sfgName);
        if (sfg == null) {
            sfg = SfcProviderServiceFunctionGroupAPI.readServiceFunctionGroupExecutor(sfgName);
            if (sfg != null) {
                serviceFunctionGroups.put(sfgName, sfg);
            }
        }

        return sfg;
    }

}
