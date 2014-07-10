/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;


import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.
        ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.
        ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.
        ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.
        service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.
        service.function.type.SftServiceFunctionNameBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.
        service.function.type.SftServiceFunctionNameKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * This class gets called whenever there is a change to
 * a Service Function list entry, i.e.,
 * added/deleted/modified.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderSfEntryDataListener implements DataChangeListener  {
    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfEntryDataListener.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        /* If any service function was deleted we need to remove it from the
         * ServiceFunctionType list.
         */
        /*
        DataObject dataOriginalObject = change.getOriginalConfigurationSubtree();
        LOG.info("\n########## getOriginalConfigurationSubtree");
        if (dataOriginalObject instanceof ServiceFunction) {
            ServiceFunction origServiceFunction = (ServiceFunction)dataOriginalObject;
            deleteServiceFunctionTypeEntry (origServiceFunction);
            LOG.info("\n########## OriginalConfigurationSubstree {}  {}",
                        origServiceFunction.getType(), origServiceFunction.getName());
        }
        */

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalConfigurationData();
        LOG.info("\n########## getOriginalConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                odlSfc.executor.execute(SfcProviderServiceTypeAPI.getSfcProviderDeleteServiceType(originalServiceFunction));
                //deleteServiceFunctionTypeEntry(originalServiceFunction);
                LOG.info("\n########## getOriginalConfigurationData {}  {}",
                        originalServiceFunction.getType(), originalServiceFunction.getName());
            }
            //System.out.println(entry.getKey() + "/" + entry.getValue());
        }
        /*
        Set<InstanceIdentifier<?>> removedConfig;
        removedConfig = change.getRemovedConfigurationData();
          for (InstanceIdentifier<?> instanceIdentifier : removedConfig) {
            Class<? extends DataObject> datatestObject = instanceIdentifier.getTargetType();
            String name = instanceIdentifier.getTargetType().getName();
            if (name.equals(OpendaylightSfc.serviceFunctionIIDName)) {
                KeyedInstanceIdentifier keyed = (KeyedInstanceIdentifier) instanceIdentifier;
                ServiceFunctionKey serviceFunctionKey = (ServiceFunctionKey) keyed.getKey();
                String sfName = serviceFunctionKey.getName();
                LOG.info("\n########## Working {} ", sfName);
            }
        }
        */

        DataObject dataUpdatedObject = change.getUpdatedConfigurationSubtree();
        LOG.info("\n########## getUpdatedConfigurationSubtree");
        if( dataUpdatedObject instanceof  ServiceFunction) {
            ServiceFunction updatedServiceFunction = (ServiceFunction) dataUpdatedObject;
            LOG.info("\n########## UpdatedConfigurationSubstree {}  {}",
                    updatedServiceFunction.getType(), updatedServiceFunction.getName());

        }

        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedConfigurationData();
        LOG.info("\n########## getCreatedConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction createdServiceFunction = (ServiceFunction) entry.getValue();
                LOG.info("\n########## getCreatedConfigurationData {}  {}",
                            createdServiceFunction.getType(), createdServiceFunction.getName());
            }
            //System.out.println(entry.getKey() + "/" + entry.getValue());
        }


        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedConfigurationData();
        LOG.info("\n########## getUpdatedConfigurationData");

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet())
        {
            if( entry.getValue() instanceof  ServiceFunction) {
                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                //createServiceFunctionTypeEntry(updatedServiceFunction);
                odlSfc.executor.execute(SfcProviderServiceTypeAPI.getSfcProvideCreateServiceType(updatedServiceFunction));
                LOG.info("\n########## getUpdatedConfigurationData {}  {}",
                        updatedServiceFunction.getType(), updatedServiceFunction.getName());
            }
            //System.out.println(entry.getKey() + "/" + entry.getValue());
        }

        // Debug and Unit Test. We trigger the unit test code by adding a service function to the datastore.
        if (SfcProviderDebug.ON) {
            SfcProviderDebug.ON = false;
            SfcProviderUnitTest.sfcProviderUnitTest(SfcProviderRpc.getSfcProviderRpc());
        }

        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    private void createServiceFunctionTypeEntry (ServiceFunction serviceFunction) {


        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        String sfkey = serviceFunction.getType();
        ServiceFunctionTypeKey  serviceFunctionTypeKey = new ServiceFunctionTypeKey(sfkey);

        //Build the instance identifier all the way down to the bottom child

        SftServiceFunctionNameKey sftServiceFunctionNameKey = new SftServiceFunctionNameKey(serviceFunction.getName());
/*
        InstanceIdentifier<SftServiceFunctionName> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
               .child(ServiceFunctionType.class, serviceFunctionTypeKey)
                .child(SftServiceFunctionName.class,sftServiceFunctionNameKey).build();


        InstanceIdentifier<ServiceFunctionType> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class)
                .child(ServiceFunctionType.class, serviceFunctionTypeKey).build();

*/


        InstanceIdentifier<ServiceFunctionTypes> sftentryIID;
        sftentryIID = InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

        // Create a item in the list keyed by service function name
        SftServiceFunctionNameBuilder sftServiceFunctionNameBuilder = new SftServiceFunctionNameBuilder();
        sftServiceFunctionNameBuilder = sftServiceFunctionNameBuilder.setName(serviceFunction.getName());
        SftServiceFunctionName sftServiceFunctionName = sftServiceFunctionNameBuilder.build();

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

        final DataModificationTransaction t = odlSfc.dataProvider
                .beginTransaction();

        t.putConfigurationData(sftentryIID, serviceFunctionTypes);
        try {
            t.commit().get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Failed to update-function, operational otherwise", e);
        }

        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    private void deleteServiceFunctionTypeEntry (ServiceFunction serviceFunction) {

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

}
