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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class has the APIs to operate on the ServiceFunction
 * datastore.
 * <p/>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 * @since 2014-06-30
 */
public class SfcProviderServiceFunctionAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceFunctionAPI.class);

    SfcProviderServiceFunctionAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceFunctionAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceFunctionAPI getDeleteServicePathFromServiceFunctionState(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteServicePathFromServiceFunctionState");
    }

    public static SfcProviderServiceFunctionAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "putServiceFunction");
    }

    public static SfcProviderServiceFunctionAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readServiceFunction");
    }

    public static SfcProviderServiceFunctionAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteServiceFunction");
    }

    public static SfcProviderServiceFunctionAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "putAllServiceFunctions");
    }

    public static SfcProviderServiceFunctionAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "readAllServiceFunctions");
    }

    public static SfcProviderServiceFunctionAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceFunctionAPI(params, paramsTypes, "deleteAllServiceFunctions");
    }

    public static ServiceFunctionState readServiceFunctionState(String serviceFunctionName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        ServiceFunctionState serviceFunctionState;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionState> dataSfcStateObject = null;
        try {
            dataSfcStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (dataSfcStateObject != null &&
                (dataSfcStateObject.get() instanceof ServiceFunctionState)) {
            serviceFunctionState = dataSfcStateObject.get();
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionState;
        } else {
            LOG.error("\n########## Could not find Service Function State for service function {}",
                    serviceFunctionName);
            return null;
        }
    }

    public static void deleteServiceFunctionState(String serviceFunctionName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);


        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, sfStateIID);
        writeTx.commit();
    }

    /*
     * We add the path name to the operational store of each SF in the path.
     */
    public static void addPathToServiceFunctionState(ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
        ArrayList<String> sfcServiceFunctionPathArrayList = new ArrayList<>();
        sfcServiceFunctionPathArrayList.add(serviceFunctionPath.getName());

        serviceFunctionStateBuilder.setSfServiceFunctionPath(sfcServiceFunctionPathArrayList);

        List<ServicePathHop> servicePathHopList = serviceFunctionPath.getServicePathHop();
        for (ServicePathHop servicePathHop : servicePathHopList) {
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(servicePathHop.getServiceFunctionName());
            InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
                    .child(ServiceFunctionState.class, serviceFunctionStateKey).build();
            serviceFunctionStateBuilder.setName(servicePathHop.getServiceFunctionName());

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                    sfStateIID, serviceFunctionStateBuilder.build(), true);
            writeTx.commit();

            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        }
    }

    protected static boolean putServiceFunction(ServiceFunction sf) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION,
                    sfEntryIID, sf, true);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected static boolean mergeServiceFunction(ServiceFunction sf) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfEntryIID, sf, true);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected ServiceFunction readServiceFunction(String serviceFunctionName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunction sf = null;
        InstanceIdentifier<ServiceFunction> sfIID;
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        sfIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).build();

        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunction> serviceFunctionDataObject = null;
            try {
                serviceFunctionDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (serviceFunctionDataObject != null
                    && serviceFunctionDataObject.isPresent()) {
                sf = serviceFunctionDataObject.get();
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return sf;
    }

    protected boolean deleteServiceFunction(String serviceFunctionName) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey).toInstance();

        if (odlSfc.getDataProvider() != null) {
            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfEntryIID);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected boolean putAllServiceFunctions(ServiceFunctions sfs) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, sfsIID, sfs);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected ServiceFunctions readAllServiceFunctions() {
        ServiceFunctions sfs = null;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).toInstance();

        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctions> serviceFunctionsDataObject = null;
            try {
                serviceFunctionsDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (serviceFunctionsDataObject != null
                    && serviceFunctionsDataObject.isPresent()) {
                sfs = serviceFunctionsDataObject.get();
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return sfs;
    }

    protected boolean deleteAllServiceFunctions() {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctions> sfsIID = InstanceIdentifier.builder(ServiceFunctions.class).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfsIID);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    /*
     * When a Service Path is deleted directly (not as a consequence of deleting a SF), we need
     * to remove its reference from all the ServiceFunction states.
     */
    @SuppressWarnings("unused")
    public void deleteServicePathFromServiceFunctionState(ServiceFunctionPath serviceFunctionPath) {

        List<ServicePathHop> sfpServiceFunctionList = serviceFunctionPath.getServicePathHop();
        for (ServicePathHop sfpServiceFunction : sfpServiceFunctionList) {
            String serviceFunctionName = sfpServiceFunction.getServiceFunctionName();
            ServiceFunctionState serviceFunctionState = readServiceFunctionState(serviceFunctionName);
            ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(serviceFunctionName);
            InstanceIdentifier<ServiceFunctionState> sfStateIID =
                    InstanceIdentifier.builder(ServiceFunctionsState.class)
                            .child(ServiceFunctionState.class, serviceFunctionStateKey)
                            .build();

            List<String> sfServiceFunctionPathList = serviceFunctionState.getSfServiceFunctionPath();
            List<String> newPathList = new ArrayList<>();
            newPathList.addAll(sfServiceFunctionPathList);
            newPathList.remove(serviceFunctionPath.getName());
            ServiceFunctionStateBuilder serviceFunctionStateBuilder = new ServiceFunctionStateBuilder();
            serviceFunctionStateBuilder.setName(serviceFunctionName);
            serviceFunctionStateBuilder.setSfServiceFunctionPath(newPathList);


            ReadWriteTransaction writeTx = odlSfc.getDataProvider().newReadWriteTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, sfStateIID, serviceFunctionStateBuilder.build(), true);
            writeTx.commit();
        }
    }
}
