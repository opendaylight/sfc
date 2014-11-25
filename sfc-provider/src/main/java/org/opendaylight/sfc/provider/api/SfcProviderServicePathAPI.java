/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.SfcProviderRestAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfpEntryDataListener
 * <p/>
 * <p/>
 * <p/>
 * @since       2014-06-30
 */
public class SfcProviderServicePathAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);
    static final Comparator<SfcServiceFunction> SF_ORDER =
            new Comparator<SfcServiceFunction>() {
                public int compare(SfcServiceFunction e1, SfcServiceFunction e2) {
                    return e2.getOrder().compareTo(e1.getOrder());
                }
            };

    SfcProviderServicePathAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "putServiceFunctionPath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readServiceFunctionPath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServiceFunctionPath");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "putAllServiceFunctionPaths");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "readAllServiceFunctionPaths");
    }
    @SuppressWarnings("unused")
    public static SfcProviderServicePathAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteAllServiceFunctionPaths");
    }

    public static  SfcProviderServicePathAPI getDeleteServicePathContainingFunction (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getDeleteServicePathInstantiatedFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getUpdateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServiceFunctionPathEntry");
    }
    @SuppressWarnings("unused")
    public static  SfcProviderServicePathAPI getUpdateServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathContainingFunction");
    }
    public static SfcProviderServicePathAPI getCheckServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "checkServiceFunctionPath");
    }


    @SuppressWarnings("unused")
    public static int numCreatedPathGetValue() {
        return numCreatedPath.get();
    }

    public int numCreatedPathIncrementGet() {
        return numCreatedPath.incrementAndGet();
    }
    @SuppressWarnings("unused")
    public int numCreatedPathDecrementGet() {
        return numCreatedPath.decrementAndGet();
    }


    @SuppressWarnings("unused")
    protected boolean putServiceFunctionPath(ServiceFunctionPath sfp) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionPath> sfpEntryIID =
                    InstanceIdentifier.builder(ServiceFunctionPaths.class).
                    child(ServiceFunctionPath.class, sfp.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfpEntryIID, sfp, true);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionPath readServiceFunctionPath(String serviceFunctionPathName) {
        printTraceStart(LOG);
        ServiceFunctionPath sfp = null;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionPath> serviceFunctionPathDataObject;
            try {
                serviceFunctionPathDataObject = readTx.read(LogicalDatastoreType
                        .CONFIGURATION, sfpIID).get();
                if (serviceFunctionPathDataObject != null
                        && serviceFunctionPathDataObject.isPresent()) {
                    sfp = serviceFunctionPathDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Path configuration data \n");
            }
        }
        printTraceStop(LOG);
        return sfp;
    }

    /**
     * This function deletes a SFP from the datastore
     * <p>
     * @param serviceFunctionPathName SFP name
     * @return Nothing.
     */
    public static boolean deleteServiceFunctionPath(String serviceFunctionPathName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        InstanceIdentifier<ServiceFunctionPath> sfpEntryIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).toInstance();

        if (!SfcDataStoreAPI.deleteTransactionAPI(sfpEntryIID, LogicalDatastoreType.CONFIGURATION)) {
            LOG.error("Failed to delete SFP: {}", serviceFunctionPathName);
        } else {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionPaths(ServiceFunctionPaths sfps) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.
                    builder(ServiceFunctionPaths.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfpsIID, sfps);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionPaths readAllServiceFunctionPaths() {
        ServiceFunctionPaths sfps = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionPaths> sfpsIID = InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionPaths> serviceFunctionPathsDataObject = null;
            try {
                serviceFunctionPathsDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read top-level Service Function Path " +
                        "container \n");
            }
            if (serviceFunctionPathsDataObject != null
                    && serviceFunctionPathsDataObject.isPresent()) {
                sfps = serviceFunctionPathsDataObject.get();
            }
        }
        printTraceStop(LOG);
        return sfps;
    }

    protected boolean deleteAllServiceFunctionPaths() {
        boolean ret = false;
        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPaths> sfpsIID =
                InstanceIdentifier.builder(ServiceFunctionPaths.class).toInstance();

        if (SfcDataStoreAPI.deleteTransactionAPI(sfpsIID, LogicalDatastoreType.CONFIGURATION)) {
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }


    @SuppressWarnings("unused")
    protected void updateServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {
        this.createServiceFunctionPathEntry(serviceFunctionPath);
    }

    /*
     * This function is actually an updated to a previously created SFP where only
     * the service chain name was given. In this function we patch the SFP with the
     * names of the chosen SFs
     */
    /**
     * This function is called whenever a SFP is created or updated. It recomputes
     * the SFP information and merges any missing data
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     */
    protected void createServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);

        long pathId;
        short posIndex = 0;
        int serviceIndex;
        ServiceFunctionChain serviceFunctionChain = null;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        try {
            serviceFunctionChain = serviceFunctionChainName != null ?
                    (ServiceFunctionChain) odlSfc.executor
                            .submit(SfcProviderServiceChainAPI.getRead(
                                    new Object[]{serviceFunctionChainName},
                                    new Class[]{String.class})).get(): null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(" \n Could not read Service Function Chain configuration for Service Path {}",
                    serviceFunctionPath.getName());
        }
        if (serviceFunctionChain == null) {
            LOG.error("\n ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }

        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ArrayList<ServicePathHop> servicePathHopArrayList = new ArrayList<>();
        ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        //List<SfcServiceFunction> sfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(serviceFunctionChain.getSfcServiceFunction());

        Collections.sort(sfcServiceFunctionList, Collections.reverseOrder(SF_ORDER));
        serviceIndex = sfcServiceFunctionList.size();
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType;
            try {
                serviceFunctionType = (ServiceFunctionType) odlSfc.executor.submit(SfcProviderServiceTypeAPI.getRead(
                        new Object[]{sfcServiceFunction.getType()}, new Class[]{String.class})).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(" Could not get list of Service Functions of type {} \n", sfcServiceFunction.getType());
                return;
            }
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                        // TODO: API to select suitable Service Function
                        String serviceFunctionName = sftServiceFunctionName.getName();
                        ServiceFunction serviceFunction = null;
                        try {
                            serviceFunction =
                                    (ServiceFunction) odlSfc.executor.submit(SfcProviderServiceFunctionAPI
                                            .getRead(new Object[]{serviceFunctionName}, new Class[]{String.class})).get();
                        } catch (InterruptedException | ExecutionException e) {
                            LOG.error(" Could not read Service Function {} " +
                                    "\n", serviceFunctionName);
                        }
                        if (serviceFunction != null) {
                            servicePathHopBuilder.setHopNumber(posIndex)
                                    .setServiceFunctionName(serviceFunctionName)
                                    .setServiceIndex((short) serviceIndex)
                                    .setServiceFunctionForwarder(serviceFunction.getSfDataPlaneLocator()
                                            .get(0)
                                            .getServiceFunctionForwarder());
                            servicePathHopArrayList.add(posIndex, servicePathHopBuilder.build());
                            serviceIndex--;
                            posIndex++;
                            break;
                        } else {
                            LOG.error("\n####### Could not find suitable SF of type in data store: {}",
                                    sfcServiceFunction.getType());
                            return;
                        }
                    }
                } else {
                    LOG.error("Could not create path because there are no configured SFs of type: {}", sfcServiceFunction.getType());
                    return;
                }
            } else {
                LOG.error("Could not create path because there are no configured SFs of type: {}", sfcServiceFunction.getType());
                return;
            }

        }

        //Build the service function path so it can be committed to datastore


        pathId = (serviceFunctionPath.getPathId() != null)  ?  serviceFunctionPath.getPathId()
                : numCreatedPathIncrementGet();
        serviceFunctionPathBuilder.setServicePathHop(servicePathHopArrayList);
        if (serviceFunctionPath.getName().isEmpty())  {
            serviceFunctionPathBuilder.setName(serviceFunctionChainName + "-Path-" + pathId);
        } else {
            serviceFunctionPathBuilder.setName(serviceFunctionPath.getName());

        }

        serviceFunctionPathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        serviceFunctionPathBuilder.setStartingIndex((short) servicePathHopArrayList.size());
        serviceFunctionPathBuilder.setServiceChainName(serviceFunctionChainName);

        ServiceFunctionPathKey serviceFunctionPathKey = new
                ServiceFunctionPathKey(serviceFunctionPathBuilder.getName());
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        ServiceFunctionPath newServiceFunctionPath =
                serviceFunctionPathBuilder.build();
        if (!SfcDataStoreAPI.writeMergeTransactionAPI(sfpIID, newServiceFunctionPath, LogicalDatastoreType.CONFIGURATION)) {
            LOG.debug("Failed to create Service Function Path: {}",
                    serviceFunctionPath.getName());
        }

        /* Prepare REST invocation */

        invokeServicePathRest(serviceFunctionPath, HttpMethod.PUT);

        printTraceStop(LOG);

    }

    /**
     * Check a SFF for consistency after datastore creation
     * <p>
     * @param serviceFunctionPath SFP object
     * @param operation HttpMethod
     * @return Nothing
     */
    public void checkServiceFunctionPath(ServiceFunctionPath serviceFunctionPath, String operation) {

        printTraceStart(LOG);

        invokeServicePathRest(serviceFunctionPath, operation);

        printTraceStop(LOG);
    }

    /**
     * This method decouples the SFP API from the SouthBound REST client.
     * SFP APIs call this method to convey SFP information to REST southbound
     * devices
     * <p>
     * @param serviceFunctionPath Service Function Path Object
     * @param httpMethod  HTTP method such as GET, PUT, POST..
     * @return Nothing.
     */
    private void invokeServicePathRest(ServiceFunctionPath serviceFunctionPath, String httpMethod) {

     /* Invoke SB REST API */

        if (serviceFunctionPath != null)
        {
            if (httpMethod.equals(HttpMethod.PUT))
            {
                Object[] servicePathObj = {serviceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderRestAPI.
                        getPutServiceFunctionPath(servicePathObj,
                                servicePathClass));
            } else if (httpMethod.equals(HttpMethod.DELETE))
            {
                Object[] servicePathObj = {serviceFunctionPath};
                Class[] servicePathClass = {ServiceFunctionPath.class};
                odlSfc.executor.execute(SfcProviderRestAPI.
                        getDeleteServiceFunctionPath(servicePathObj,
                                servicePathClass));
            }
        } else {
            LOG.error("Could not find Service Function path: {}", serviceFunctionPath.getName());
        }

    }


    /*
     * We iterate through all service paths that use this service function and remove them.
     * In the end since there is no more operational state, we remove the state tree.
     */

    @SuppressWarnings("unused")
    public boolean deleteServicePathContainingFunction (ServiceFunction serviceFunction) {

        printTraceStart(LOG);
        boolean ret = true;
        ServiceFunctionState serviceFunctionState;

        serviceFunctionState = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (serviceFunctionState != null) {
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfServiceFunctionPathList) {

                if (deleteServiceFunctionPath(pathName)) {
                    ret = ret && true;
                } else {
                    ret = ret && false;
                }
            }
        } else {
            LOG.debug("Could not find Service function Paths using Service Function: {} ",
                    serviceFunction.getName());
        }
        printTraceStop(LOG);
        return ret;
    }


    /*
     * When a SF is updated, meaning key remains the same, but other fields change we need to
     * update all affected SFPs. We need to do that because admin can update critical fields
     * as SF type, rendering the path unfeasible. The update reads the current path from
     * data store, keeps pathID intact and rebuild the SF list.
     *
     * The update can or not work.
     */
    private void updateServicePathContainingFunction (ServiceFunction serviceFunction) {

        printTraceStart(LOG);

        InstanceIdentifier<ServiceFunctionPath> sfpIID;

        ServiceFunctionState serviceFunctionState = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (serviceFunctionState != null) {
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            for (String pathName : sfServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
                Optional<ServiceFunctionPath> serviceFunctionPathObject;
                try {
                    serviceFunctionPathObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpIID).get();
                    if (serviceFunctionPathObject != null &&
                            (serviceFunctionPathObject.get() instanceof  ServiceFunctionPath)) {
                        ServiceFunctionPath servicefunctionPath = serviceFunctionPathObject.get();
                        createServiceFunctionPathEntry(servicefunctionPath);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Could not read Service Function Path configuration data \n");
                }
            }
        } else {
            LOG.error("Failed to get reference to Service Function State {} ", serviceFunction.getName());
        }
        printTraceStop(LOG);
        return;
    }
}
