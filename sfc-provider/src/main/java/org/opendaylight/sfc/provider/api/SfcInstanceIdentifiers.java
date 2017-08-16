/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.api;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.ServiceFunctionGroups;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This the main SFC Instance Identifier Constants class.
 *
 * <p>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */

public final class SfcInstanceIdentifiers implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcInstanceIdentifiers.class);

    public static final InstanceIdentifier<ServiceFunctionChain> SFC_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionChains.class).child(ServiceFunctionChain.class).build();

    public static final InstanceIdentifier<ServiceFunctionClassifier> SCF_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionClassifiers.class).child(ServiceFunctionClassifier.class).build();

    public static final InstanceIdentifier<ServiceFunction> SF_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctions.class).child(ServiceFunction.class).build();

    public static final InstanceIdentifier<ServiceFunctionGroup> SFG_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionGroups.class).child(ServiceFunctionGroup.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarder> SFF_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class).build();

    public static final InstanceIdentifier<ServiceFunctionPath> SFP_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionPaths.class).child(ServiceFunctionPath.class).build();

    public static final InstanceIdentifier<RenderedServicePath> RSP_ENTRY_IID = InstanceIdentifier
            .builder(RenderedServicePaths.class).child(RenderedServicePath.class).build();

    public static final InstanceIdentifier<ServiceFunctionSchedulerType> SFST_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionSchedulerTypes.class).child(ServiceFunctionSchedulerType.class).build();

    public static final InstanceIdentifier<ServiceFunctionChains> SFC_IID = InstanceIdentifier
            .builder(ServiceFunctionChains.class).build();

    public static final InstanceIdentifier<ServiceFunctionTypes> SFT_IID = InstanceIdentifier
            .builder(ServiceFunctionTypes.class).build();

    public static final InstanceIdentifier<Acl> ACL_ENTRY_IID = InstanceIdentifier.builder(AccessLists.class)
            .child(Acl.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarders> SFF_IID = InstanceIdentifier
            .builder(ServiceFunctionForwarders.class).build();

    public static final InstanceIdentifier<ServiceFunctions> SF_IID = InstanceIdentifier.builder(ServiceFunctions.class)
            .build();

    public static final InstanceIdentifier<ServiceFunctionPaths> SFP_IID = InstanceIdentifier
            .builder(ServiceFunctionPaths.class).build();

    public static final InstanceIdentifier<ServiceFunctionState> SFSTATE_ENTRY_IID = InstanceIdentifier
            .builder(ServiceFunctionsState.class).child(ServiceFunctionState.class).build();

    public SfcInstanceIdentifiers() {
        LOG.info("Opendaylight Service Function Instance Identifiers Constants Initialized");
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        final InstanceIdentifier<ServiceFunctionClassifiers> scfIid = InstanceIdentifier
                .builder(ServiceFunctionClassifiers.class).build();

        final InstanceIdentifier<ServiceFunctionGroups> sfcIid = InstanceIdentifier.builder(ServiceFunctionGroups.class)
                .build();

        final InstanceIdentifier<RenderedServicePaths> rspIid = InstanceIdentifier.builder(RenderedServicePaths.class)
                .build();

        final InstanceIdentifier<AccessLists> aclIid = InstanceIdentifier.builder(AccessLists.class).build();

        final InstanceIdentifier<ServiceFunctionSchedulerTypes> sfstIid = InstanceIdentifier
                .builder(ServiceFunctionSchedulerTypes.class).build();

        final InstanceIdentifier<ServiceFunctionsState> sfstateIid = InstanceIdentifier
                .builder(ServiceFunctionsState.class).build();

        SfcDataStoreAPI.deleteTransactionAPI(SFC_IID, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(scfIid, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(SFT_IID, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(SF_IID, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(sfcIid, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(SFF_IID, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(SFP_IID, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(rspIid, LogicalDatastoreType.OPERATIONAL);
        SfcDataStoreAPI.deleteTransactionAPI(aclIid, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(sfstIid, LogicalDatastoreType.CONFIGURATION);
        SfcDataStoreAPI.deleteTransactionAPI(sfstateIid, LogicalDatastoreType.OPERATIONAL);
    }
}
