/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.ServiceFunctionGroups;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.type.definition.SupportedDataplanelocatorTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.type.definition.SupportedDataplanelocatorTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Gre;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mac;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Mpls;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This the main SFC Provider class. It is instantiated from the SFCProviderModule class.
 * <p>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
// FIXME Make this class a singleton or remove static getOpendaylightSfcObj
public class OpendaylightSfc implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightSfc.class);
    protected static DataBroker dataProvider;

    /* Constructors */
    public OpendaylightSfc(final DataBroker dataProvider) {
        this.dataProvider = dataProvider;
        initServiceFunctionTypes();
    }

    /* Accessors */

    private void initServiceFunctionTypes() {
        @SuppressWarnings("serial")
        List<SupportedDataplanelocatorTypes> supportedDplTypes = new ArrayList<SupportedDataplanelocatorTypes>() {

            {
                add(new SupportedDataplanelocatorTypesBuilder().setDataplanelocatorType(VxlanGpe.class).build());
                add(new SupportedDataplanelocatorTypesBuilder().setDataplanelocatorType(Mac.class).build());
                add(new SupportedDataplanelocatorTypesBuilder().setDataplanelocatorType(Gre.class).build());
                add(new SupportedDataplanelocatorTypesBuilder().setDataplanelocatorType(Mpls.class).build());
                add(new SupportedDataplanelocatorTypesBuilder().setDataplanelocatorType(Other.class).build());
            }
        };

        @SuppressWarnings("serial")
        List<String> types = new ArrayList<String>() {

            {
                add("firewall");
                add("dpi");
                add("napt44");
                add("qos");
                add("ids");
                add("http-header-enrichment");
                add("tcp-proxy");
            }
        };

        List<ServiceFunctionType> sftList = new ArrayList<>();

        for (String type : types) {
            SftTypeName sftType = new SftTypeName(type);
            ServiceFunctionTypeBuilder sftBuilder = new ServiceFunctionTypeBuilder()
                .setKey(new ServiceFunctionTypeKey(sftType))
                .setNshAware(true)
                .setSymmetry(true)
                .setBidirectionality(true)
                .setRequestReclassification(true)
                .setSupportedDataplanelocatorTypes(supportedDplTypes)
                .setType(sftType);
            sftList.add(sftBuilder.build());
        }
        ServiceFunctionTypesBuilder sftTypesBuilder = new ServiceFunctionTypesBuilder().setServiceFunctionType(sftList);
        if (SfcDataStoreAPI.writePutTransactionAPI(SfcProviderUtils.SFT_IID, sftTypesBuilder.build(),
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.info("Initialised Service Function Types");
        } else {
            LOG.error("Could not initialise Service Function Types");
        }
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!

        if (dataProvider != null) {

            final InstanceIdentifier<ServiceFunctionClassifiers> SCF_IID =
                    InstanceIdentifier.builder(ServiceFunctionClassifiers.class).build();

            final InstanceIdentifier<ServiceFunctionGroups> SFG_IID =
                    InstanceIdentifier.builder(ServiceFunctionGroups.class).build();

            final InstanceIdentifier<RenderedServicePaths> rspIid =
                    InstanceIdentifier.builder(RenderedServicePaths.class).build();

            final InstanceIdentifier<AccessLists> aclIid = InstanceIdentifier.builder(AccessLists.class).build();

            final InstanceIdentifier<ServiceFunctionSchedulerTypes> sfstIid =
                    InstanceIdentifier.builder(ServiceFunctionSchedulerTypes.class).build();

            final InstanceIdentifier<ServiceFunctionsState> sfstateIid =
                    InstanceIdentifier.builder(ServiceFunctionsState.class).build();

            SfcDataStoreAPI.deleteTransactionAPI(SfcProviderUtils.SFC_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SCF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SfcProviderUtils.SFT_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SfcProviderUtils.SF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFG_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SfcProviderUtils.SFF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SfcProviderUtils.SFP_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(rspIid, LogicalDatastoreType.OPERATIONAL);
            SfcDataStoreAPI.deleteTransactionAPI(aclIid, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(sfstIid, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(sfstateIid, LogicalDatastoreType.OPERATIONAL);
        }
    }
}
