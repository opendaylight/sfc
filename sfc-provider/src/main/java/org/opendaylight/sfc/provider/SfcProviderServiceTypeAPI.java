/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * This class has the APIs to operate on the ServiceFunctionType
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
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderServiceTypeAPI implements Runnable {

    private ServiceFunction serviceFunction;
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    public enum OperationType {CREATE, DELETE}

    private OperationType operation = OperationType.CREATE;

    SfcProviderServiceTypeAPI (ServiceFunction sf, OperationType type) {
        this.serviceFunction = sf;
        this.operation = type;
    }


    public static  SfcProviderServiceTypeAPI getSfcProviderDeleteServiceType(ServiceFunction sf) {
        return new SfcProviderServiceTypeAPI(sf, OperationType.DELETE);
    }


    public static  SfcProviderServiceTypeAPI getSfcProvideCreateServiceType(ServiceFunction sf) {
        return new SfcProviderServiceTypeAPI(sf, OperationType.CREATE);
    }

    @Override
    public void run() {
        switch (operation) {
            case CREATE:
                createServiceFunctionTypeEntry(serviceFunction);
                break;
            case DELETE:
                deleteServiceFunctionTypeEntry(serviceFunction);
                break;
        }
    }



    public void createServiceFunctionTypeEntry (ServiceFunction serviceFunction) {


        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child

        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());

        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
               .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class,sftServiceFunctionNameKey).build();


        // Create a item in the list keyed by service function name
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder.setName(serviceFunction.getName());
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();

        /*
        InstanceIdentifier<ServiceFunctionType> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();


        InstanceIdentifier<ServiceFunctionTypes> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

        ArrayList<SftServiceFunctionName> sftServiceFunctionNameArrayList = new ArrayList<>();
        sftServiceFunctionNameArrayList.add(sftServiceFunctionName);

        ServiceFunctionTypeBuilder serviceFunctionTypeBuilder = new ServiceFunctionTypeBuilder();
        serviceFunctionTypeBuilder = serviceFunctionTypeBuilder.setType(serviceFunction.getType());
        serviceFunctionTypeBuilder.setSftServiceFunctionName(sftServiceFunctionNameArrayList);
        ServiceFunctionType serviceFunctionType = serviceFunctionTypeBuilder.build();

        ArrayList<ServiceFunctionType> serviceFunctionTypeArrayList = new ArrayList<>();
        serviceFunctionTypeArrayList.add(serviceFunctionTypeBuilder.build());


        ServiceFunctionTypesBuilder serviceFunctionTypesBuilder = new ServiceFunctionTypesBuilder();
        serviceFunctionTypesBuilder.setServiceFunctionType(serviceFunctionTypeArrayList);
        ServiceFunctionTypes serviceFunctionTypes = serviceFunctionTypesBuilder.build();

        t.putConfigurationData(sftentryIID, serviceFunctionTypes);

        */
        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.putConfigurationData(sftentryIID, sftServiceFunctionName);

        try {
            t.commit().get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Failed to update-function, operational otherwise", e);
        }

        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    public void deleteServiceFunctionTypeEntry (ServiceFunction serviceFunction) {

        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey  serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).
                child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class,sftServiceFunctionNameKey).toInstance();

        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();
        t.removeConfigurationData(sftentryIID);
        try {
            t.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("deleteServiceFunction failed", e);
        }
    }

    public static ServiceFunctionType readServiceFunctionType (String type) {
        InstanceIdentifier<ServiceFunctionType> sftentryIID;
        ServiceFunctionTypeKey serviceFunctionTypeKey;
        serviceFunctionTypeKey = new ServiceFunctionTypeKey(type);

            /*
             * We iterate thorough the list of service function types and for each one we get a suitable
             * Service Function
             */
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();
        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sftentryIID);
        if (dataObject instanceof ServiceFunctionType) {
            ServiceFunctionType serviceFunctionType = (ServiceFunctionType) dataObject;
            return serviceFunctionType;
        } else {
            return null;
        }

    }
}
