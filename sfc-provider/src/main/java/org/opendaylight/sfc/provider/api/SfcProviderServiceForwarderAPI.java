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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
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
 * <p/>
 * <p/>
 * <p/>
 * @since 2014-06-30
 */
public class SfcProviderServiceForwarderAPI extends SfcProviderAbstractAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceForwarderAPI.class);

    SfcProviderServiceForwarderAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderServiceForwarderAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }


    public static SfcProviderServiceForwarderAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "putServiceFunctionForwarder");
    }

    public static SfcProviderServiceForwarderAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "readServiceFunctionForwarder");
    }

    public static SfcProviderServiceForwarderAPI getDelete(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteServiceFunctionForwarder");
    }

    public static SfcProviderServiceForwarderAPI getPutAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "putAllServiceFunctionForwarders");
    }

    public static SfcProviderServiceForwarderAPI getReadAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "readAllServiceFunctionForwarders");
    }

    public static SfcProviderServiceForwarderAPI getDeleteAll(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteAllServiceFunctionForwarders");
    }

    public static SfcProviderServiceForwarderAPI getDeleteServiceFunctionFromForwarder(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "deleteServiceFunctionFromForwarder");
    }

    public static SfcProviderServiceForwarderAPI getCreateServiceForwarderAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "createServiceFunctionForwarder");
    }

    public static SfcProviderServiceForwarderAPI getUpdateServiceForwarderAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceForwarderAPI(params, paramsTypes, "updateServiceFunctionForwarder");
    }

    /*
    public static ServiceFunctionForwarder readServiceFunctionForwarder(String name) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(name);
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionForwarder> serviceFunctionForwarderObject = null;
        try {
            serviceFunctionForwarderObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sffIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionForwarderObject != null &&
                (serviceFunctionForwarderObject.get() instanceof ServiceFunctionForwarder)) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionForwarderObject.get();
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }
    */

    public static void addPathIdtoServiceFunctionForwarder(ServiceFunctionPath serviceFunctionPath) throws ExecutionException, InterruptedException {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        InstanceIdentifier<ServiceFunctionForwarders> sffsIID;
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        ArrayList<ServiceFunctionForwarder> serviceFunctionForwarderList = new ArrayList<>();
        List<ServicePathHop> servicePathHopList = serviceFunctionPath.getServicePathHop();

        for (ServicePathHop sfpServiceFunction : servicePathHopList) {

            ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                    new ServiceFunctionForwarderKey(sfpServiceFunction.getServiceFunctionForwarder());
            ServiceFunctionForwarder serviceFunctionForwarder =
                    (ServiceFunctionForwarder) odlSfc.executor.submit(SfcProviderServiceForwarderAPI.getRead(
                            new Object[]{sfpServiceFunction.getServiceFunctionForwarder()},
                            new Class[]{String.class})).get();
            ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
            if (serviceFunctionForwarder != null) {
                //serviceFunctionForwarderBuilder.setPathId(serviceFunctionPath.getPathId());
                serviceFunctionForwarderBuilder.setName(sfpServiceFunction.getServiceFunctionForwarder());
                serviceFunctionForwarderBuilder.setSffDataPlaneLocator(serviceFunctionForwarder.getSffDataPlaneLocator());
                serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionForwarder.getServiceFunctionDictionary());
                serviceFunctionForwarderBuilder.setKey(serviceFunctionForwarderKey);

            } else {
                LOG.error("Failed to read Service Function Forwarder from data store");
                continue;
            }
            serviceFunctionForwarderList.add(serviceFunctionForwarderBuilder.build());
        }

        serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);
        sffsIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sffsIID, serviceFunctionForwardersBuilder.build(), true);
        writeTx.commit();

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

        //serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);

    }

    // TODO: need to check for sff-data-plane-locator
    /*
     * This method checks if a SFF is complete and can be sent to southbound devices
     */
    public static boolean checkServiceFunctionForwarder(ServiceFunctionForwarder serviceFunctionForwarder) {
        if ((serviceFunctionForwarder.getName() != null) &&
                (serviceFunctionForwarder.getServiceFunctionDictionary() != null)) {
            return true;
        } else {
            return false;
        }

    }

    protected boolean putServiceFunctionForwarder(ServiceFunctionForwarder sff) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                    child(ServiceFunctionForwarder.class, sff.getKey()).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sffEntryIID, sff, true);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected ServiceFunctionForwarder readServiceFunctionForwarder(String serviceFunctionForwarderName) {
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionForwarder sff = null;
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceFunctionForwarder> serviceFunctionForwarderDataObject = null;
            try {
                serviceFunctionForwarderDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sffIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (serviceFunctionForwarderDataObject != null
                    && serviceFunctionForwarderDataObject.isPresent()) {
                sff = serviceFunctionForwarderDataObject.get();
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return sff;
    }

    protected boolean deleteServiceFunctionForwarder(String serviceFunctionForwarderName) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class).
                child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey).toInstance();

        if (dataBroker != null) {
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sffEntryIID);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected boolean putAllServiceFunctionForwarders(ServiceFunctionForwarders sffs) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (dataBroker != null) {

            InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                    InstanceIdentifier.builder(ServiceFunctionForwarders.class).toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sffsIID, sffs);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected ServiceFunctionForwarders readAllServiceFunctionForwarders() {
        ServiceFunctionForwarders sffs = null;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                InstanceIdentifier.builder(ServiceFunctionForwarders.class).toInstance();

        if (odlSfc.getDataProvider() != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunctionForwarders> serviceFunctionForwardersDataObject = null;
            try {
                serviceFunctionForwardersDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sffsIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (serviceFunctionForwardersDataObject != null
                    && serviceFunctionForwardersDataObject.isPresent()) {
                sffs = serviceFunctionForwardersDataObject.get();
            }
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return sffs;
    }

    protected boolean deleteAllServiceFunctionForwarders() {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            InstanceIdentifier<ServiceFunctionForwarders> sffsIID =
                    InstanceIdentifier.builder(ServiceFunctionForwarders.class).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, sffsIID);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    /* We create a single service function forwarder from the first data-plane locator
     * on the service function.
     */
    public void createServiceFunctionForwarder(ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        String serviceFunctionForwarderName = serviceFunction.getSfDataPlaneLocator()
                .get(0).getServiceFunctionForwarder();
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .build();

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();

        ArrayList<ServiceFunctionDictionary> serviceFunctionDictionaryList = new ArrayList<>();
        ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();

        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
        sffSfDataPlaneLocatorBuilder.setLocatorType(serviceFunction.getSfDataPlaneLocator().get(0).getLocatorType());

        serviceFunctionDictionaryBuilder.setName(serviceFunction.getName()).setType(serviceFunction.getType())
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build());
        serviceFunctionDictionaryList.add(serviceFunctionDictionaryBuilder.build());

        serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionDictionaryList);

        LOG.debug("\n########## Creating Forwarder: {}  Service Function: {} "
                , serviceFunctionForwarderName, serviceFunction.getName());

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                sffIID, serviceFunctionForwarderBuilder.build(), true);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    public void deleteServiceFunctionFromForwarder(ServiceFunction serviceFunction) {
        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String serviceFunctionForwarderName = serviceFunction.getSfDataPlaneLocator()
                .get(0).getServiceFunctionForwarder();
        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey)
                .build();

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sffIID);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    @SuppressWarnings("unused")
    public void updateServiceFunctionForwarder(ServiceFunction serviceFunction) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        deleteServiceFunctionFromForwarder(serviceFunction);
        createServiceFunctionForwarder(serviceFunction);

    }

    @SuppressWarnings("unused")
    public void createServiceFunctionForwarders(ServiceFunctionChains serviceFunctionchains) {

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        InstanceIdentifier<ServiceFunctionForwarders> sffIID;

        // Prepare top container and list
        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder = new ServiceFunctionForwardersBuilder();
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .build();
        ArrayList<ServiceFunctionForwarder> serviceFunctionForwarderList = new ArrayList<>();

        List<ServiceFunctionChain> serviceFunctionChainList = serviceFunctionchains.getServiceFunctionChain();
        // Iterate through all Service Function Chains
        for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {

            // Iterate thorough all Service Functions in a single chain
            List<SfcServiceFunction> sfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
            for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {

                // Read a single Service Function
                ServiceFunction serviceFunction = (ServiceFunction) SfcProviderServiceFunctionAPI
                        .getRead(
                                new Object[]{sfcServiceFunction.getName()}, new Class[]{String.class}).call();

                // Build a single service function forwarder
                String serviceFunctionForwarderName = serviceFunction.getSfDataPlaneLocator()
                        .get(0).getServiceFunctionForwarder();
                ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                        new ServiceFunctionForwarderKey(serviceFunction.getSfDataPlaneLocator()
                                .get(0).getServiceFunctionForwarder());
                ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
                serviceFunctionForwarderBuilder.setName(serviceFunctionForwarderName);
                serviceFunctionForwarderBuilder.setKey(serviceFunctionForwarderKey);

                ArrayList<ServiceFunctionDictionary> serviceFunctionDictionaryList = new ArrayList<>();
                ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
                SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
                sffSfDataPlaneLocatorBuilder.setLocatorType(serviceFunction.getSfDataPlaneLocator().get(0).getLocatorType());

                serviceFunctionDictionaryBuilder.setName(serviceFunction.getName()).setType(serviceFunction.getType())
                        .setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build());
                serviceFunctionDictionaryList.add(serviceFunctionDictionaryBuilder.build());

                serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionDictionaryList);

                serviceFunctionForwarderList.add(serviceFunctionForwarderBuilder.build());

                LOG.debug("\n########## Creating Forwarder: {}  Service Function: {} "
                        , serviceFunctionForwarderName, serviceFunction.getName());


            }
            serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);
            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    sffIID, serviceFunctionForwardersBuilder.build(), true);
            writeTx.commit();
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    @SuppressWarnings("unused")
    public void deleteServiceFunctionForwarderfromSF(ServiceFunction serviceFunction) {

        /*
         * TODO: We assume that if a ServiceFunction exists it belongs to a ServiceFunctionForwarder
         *
         * But this is not necessarily always true since the SFF could be deleted through
         * RESTconf. So, later more checks will be necessary.
         */


        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        String serviceFunctionForwarderName = serviceFunction.getSfDataPlaneLocator()
                .get(0).getServiceFunctionForwarder();
        InstanceIdentifier<ServiceFunctionDictionary> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey =
                new ServiceFunctionForwarderKey(serviceFunctionForwarderName);
        ServiceFunctionDictionaryKey serviceFunctionDictionaryKey =
                new ServiceFunctionDictionaryKey(serviceFunction.getName());
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .child(ServiceFunctionDictionary.class, serviceFunctionDictionaryKey)
                .build();
        LOG.debug("\n########## Deleting Forwarder: {}  Service Function: {} "
                , serviceFunctionForwarderName, serviceFunction.getName());

        WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sffIID);
        writeTx.commit();
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
