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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class has the APIs to operate on the ServiceFunctionChain
 * datastore.
 *
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 *
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderServiceChainAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceChainAPI.class);

    SfcProviderServiceChainAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceChainAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceChainAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "putServiceFunctionChain");
    }

    public static SfcProviderServiceChainAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "readServiceFunctionChain");
    }

    public static SfcProviderServiceChainAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "deleteServiceFunctionChain");
    }

    public static SfcProviderServiceChainAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "putAllServiceFunctionChains");
    }

    public static SfcProviderServiceChainAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "readAllServiceFunctionChains");
    }

    public static SfcProviderServiceChainAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "deleteAllServiceFunctionChains");
    }

    public static  SfcProviderServiceChainAPI getAddChainToChainState (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceChainAPI(params, paramsTypes, "addChainToChainState");
    }

    protected boolean putServiceFunctionChain(ServiceFunctionChain serviceFunctionChain) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionChain> sfcEntryIID =
                    InstanceIdentifier.builder(ServiceFunctionChains.class)
                            .child(ServiceFunctionChain.class,
                                    serviceFunctionChain.getKey())
                            .toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sfcEntryIID, serviceFunctionChain, true);
            writeTx.commit();
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionChain readServiceFunctionChain(String serviceFunctionChainName) {
        printTraceStart(LOG);
        ServiceFunctionChain sfc = null;
        InstanceIdentifier<ServiceFunctionChain> sfcIID;
        ServiceFunctionChainKey serviceFunctionChainKey =
                new ServiceFunctionChainKey(serviceFunctionChainName);
        sfcIID = InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, serviceFunctionChainKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionChain> serviceFunctionChainDataObject;
            try {
                serviceFunctionChainDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfcIID).get();
                if (serviceFunctionChainDataObject != null
                        && serviceFunctionChainDataObject.isPresent()) {
                    sfc = serviceFunctionChainDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Chain " +
                        "configuration {}", serviceFunctionChainName);
            }
        }
        printTraceStop(LOG);
        return sfc;
    }

    protected boolean deleteServiceFunctionChain(String serviceFunctionChainName) {
        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionChainKey serviceFunctionChainKey =
                new ServiceFunctionChainKey(serviceFunctionChainName);
        InstanceIdentifier<ServiceFunctionChain> sfcEntryIID =
                InstanceIdentifier.builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, serviceFunctionChainKey).toInstance();

        if (dataBroker != null) {
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfcEntryIID);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected boolean putAllServiceFunctionChains(ServiceFunctionChains sfcs) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionChains> sfcsIID =
                    InstanceIdentifier.builder(ServiceFunctionChains.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sfcsIID, sfcs);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected ServiceFunctionChains readAllServiceFunctionChains() {
        ServiceFunctionChains sfcs = null;
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionChains> sfcsIID = InstanceIdentifier
                .builder(ServiceFunctionChains.class).toInstance();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctionChains> serviceFunctionChainsDataObject = null;
            try {
                serviceFunctionChainsDataObject = readTx
                        .read(LogicalDatastoreType.CONFIGURATION, sfcsIID).get();
                if (serviceFunctionChainsDataObject != null
                        && serviceFunctionChainsDataObject.isPresent()) {
                    sfcs = serviceFunctionChainsDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read Service Function Chains " +
                        "configuration");
            }

        }
        printTraceStop(LOG);
        return sfcs;
    }

    protected boolean deleteAllServiceFunctionChains() {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionChains> sfcsIID =
                    InstanceIdentifier.builder(ServiceFunctionChains.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sfcsIID);
            writeTx.commit();

            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected void addChainToChainState (ServiceFunctionChain serviceFunctionChain) {

        printTraceStart(LOG);
        ServiceFunctionChainStateKey serviceFunctionChainStateKey = new
                ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcoIID =
                InstanceIdentifier.builder(ServiceFunctionChainsState.class)
                .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey).build();

        ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new ServiceFunctionChainStateBuilder();
        serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                sfcoIID, serviceFunctionChainStateBuilder.build(), true);
        writeTx.commit();
        printTraceStop(LOG);
    }

    public static void addPathToServiceFunctionChainState (ServiceFunctionChain serviceFunctionChain,
                                                     ServiceFunctionPath serviceFunctionPath) {

        printTraceStart(LOG);
        ServiceFunctionChainStateKey serviceFunctionChainStateKey = new
                ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcoIID = InstanceIdentifier
                .builder(ServiceFunctionChainsState.class)
                .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey).build();

        ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new
                ServiceFunctionChainStateBuilder();
        ArrayList<String> sfcServiceFunctionPathArrayList = new ArrayList<>();
        sfcServiceFunctionPathArrayList.add(serviceFunctionPath.getName());
        serviceFunctionChainStateBuilder.setSfcServiceFunctionPath(sfcServiceFunctionPathArrayList);
        serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                sfcoIID, serviceFunctionChainStateBuilder.build(), true);
        writeTx.commit();
        printTraceStop(LOG);


    }
/*
    private InstanceIdentifier<SfcServiceFunction> getServiceFunctionIIDFromChain (ServiceFunctionChain sfc, ServiceFunction sf) {
        SfcServiceFunctionKey serviceFunctionKey = new SfcServiceFunctionKey(sf.getName());
        InstanceIdentifier<SfcServiceFunction> sfIID = InstanceIdentifier
                .builder(ServiceFunctionChains.class)
                .child(ServiceFunctionChain.class, sfc.getKey())
                .child(SfcServiceFunction.class, serviceFunctionKey).build();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<SfcServiceFunction> serviceFunctionObject = null;
        try {
            serviceFunctionObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
            if (serviceFunctionObject != null) {
                serviceFunctionObject.get();
                printTraceStop(LOG);
                return sfIID;

            } else {
                printTraceStop(LOG);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\n########## Failed to get Service Function IID " +
                            "from Chain: {}",
                    e.getMessage());
            printTraceStop(LOG);
            return null;
        }
    }

*/
    public static ServiceFunctionChains getServiceFunctionChainsRef () {
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionChains> sfcsIID;
        sfcsIID = InstanceIdentifier.builder(ServiceFunctionChains.class).build();

        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionChains> serviceFunctionChainsObject = null;
        try {
            serviceFunctionChainsObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfcsIID).get();
            if (serviceFunctionChainsObject != null) {
                printTraceStop(LOG);
                return serviceFunctionChainsObject.get();
            } else {
                LOG.error("\n########## Failed to get Service Function Chains reference: {}",
                        Thread.currentThread().getStackTrace()[1]);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\n########## Failed to get Service Function Chains reference: {}",
                    e.getMessage());
            return null;
        }
    }

    public static ServiceFunctionChainsState getServiceFunctionChainsStateRef () {
        printTraceStart(LOG);
        InstanceIdentifier<ServiceFunctionChainsState> sfcsIID;
        sfcsIID = InstanceIdentifier.builder(ServiceFunctionChainsState.class).build();

        ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
        Optional<ServiceFunctionChainsState> serviceFunctionChainStateObject = null;
        try {
            serviceFunctionChainStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfcsIID).get();
            if (serviceFunctionChainStateObject != null) {
                printTraceStop(LOG);
                return serviceFunctionChainStateObject.get();
            } else {
                LOG.error("\n########## Failed to get Service Function Chains reference: {}",
                        Thread.currentThread().getStackTrace()[1]);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("\n########## Failed to get Service Function Chains reference: {}",
                    e.getMessage());
            return null;
        }
    }
}
