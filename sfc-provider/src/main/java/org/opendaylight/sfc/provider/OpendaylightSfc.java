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
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ServiceFunctionSchedulerTypes;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This the main SFC Provider class.
 * This class should be instantiated from blueprint, so never it couldn't be a singleton.
 * <p>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
// FIXME Make this class a singleton or remove static getOpendaylightSfcObj
//          to achieve that we mustremove getOpendaylightSfcObj first we must use blueprint in every bundle
//          in order to inject this class to another bean
public class OpendaylightSfc implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightSfc.class);

    public static final InstanceIdentifier<ServiceFunctionChain> SFC_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionChains.class).child(ServiceFunctionChain.class).build();

    public static final InstanceIdentifier<ServiceFunctionClassifier> SCF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionClassifiers.class).child(ServiceFunctionClassifier.class).build();

    public static final InstanceIdentifier<ServiceFunction> SF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class).build();

    public static final InstanceIdentifier<ServiceFunctionGroup> SFG_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionGroups.class).child(ServiceFunctionGroup.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarder> SFF_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class).child(ServiceFunctionForwarder.class).build();

    public static final InstanceIdentifier<ServiceFunctionPath> SFP_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionPaths.class).child(ServiceFunctionPath.class).build();

    public static final InstanceIdentifier<RenderedServicePath> RSP_ENTRY_IID =
            InstanceIdentifier.builder(RenderedServicePaths.class).child(RenderedServicePath.class).build();

    public static final InstanceIdentifier<ServiceFunctionSchedulerType> SFST_ENTRY_IID = InstanceIdentifier
        .builder(ServiceFunctionSchedulerTypes.class).child(ServiceFunctionSchedulerType.class).build();

    public static final InstanceIdentifier<ServiceFunctionChains> SFC_IID =
            InstanceIdentifier.builder(ServiceFunctionChains.class).build();

    public static final InstanceIdentifier<ServiceFunctionTypes> SFT_IID =
            InstanceIdentifier.builder(ServiceFunctionTypes.class).build();

    public static final InstanceIdentifier<Acl> ACL_ENTRY_IID =
            InstanceIdentifier.builder(AccessLists.class).child(Acl.class).build();

    public static final InstanceIdentifier<ServiceFunctionForwarders> SFF_IID =
            InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

    public static final InstanceIdentifier<ServiceFunctions> SF_IID =
            InstanceIdentifier.builder(ServiceFunctions.class).build();

    public static final InstanceIdentifier<ServiceFunctionPaths> SFP_IID =
            InstanceIdentifier.builder(ServiceFunctionPaths.class).build();

    public static final InstanceIdentifier<ServiceFunctionState> SFSTATE_ENTRY_IID =
            InstanceIdentifier.builder(ServiceFunctionsState.class).child(ServiceFunctionState.class).build();

    public static final int EXECUTOR_THREAD_POOL_SIZE = 100;

    protected static DataBroker dataProvider=null;
    protected static BindingAwareBroker broker=null;
    private static OpendaylightSfc opendaylightSfcObj=null;
    protected static SfcThreadPoolWrapper poolWrapperInstance=null;

    public OpendaylightSfc() {
       //this class should be instantiated from blueprint.
       opendaylightSfcObj = this;
       LOG.info("Opendaylight Service Function Chaining Initialized");
    }

    /* Accessors */

    public void initServiceFunctionTypes() {
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
        if (SfcDataStoreAPI.writePutTransactionAPI(SFT_IID, sftTypesBuilder.build(),
                LogicalDatastoreType.CONFIGURATION)) {
            LOG.info("Initialized Service Function Types");
        } else {
            LOG.error("Could not initialize Service Function Types");
        }
    }

    public ExecutorService getExecutor() {
        return poolWrapperInstance.getExecutor();
    }

//setter mandatory for blueprint
    public void setThreadpoolwrapper(SfcThreadPoolWrapper t){
        this.poolWrapperInstance = t;
    }

    public void setDataProvider(DataBroker dataProvider) {
        OpendaylightSfc.dataProvider = dataProvider;
    }

    public DataBroker getDataProvider() {
        return OpendaylightSfc.dataProvider;
    }

    public void setBroker(BindingAwareBroker broker) {
        this.broker = broker;
    }

    public BindingAwareBroker getBroker() {
        return this.broker;
    }

    //This getter is 100% redundant,
    // because you could get the beano pointer with blueprint injection
    public static OpendaylightSfc getOpendaylightSfcObj() {
        return OpendaylightSfc.opendaylightSfcObj;
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

            SfcDataStoreAPI.deleteTransactionAPI(SFC_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SCF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFT_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFG_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFF_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(SFP_IID, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(rspIid, LogicalDatastoreType.OPERATIONAL);
            SfcDataStoreAPI.deleteTransactionAPI(aclIid, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(sfstIid, LogicalDatastoreType.CONFIGURATION);
            SfcDataStoreAPI.deleteTransactionAPI(sfstateIid, LogicalDatastoreType.OPERATIONAL);

            // When we close this service we don't need to shutdown external executor,
            //it's SfcThreadPoolWrapper bean responsability!
            /*
            *executor.shutdown();
            *if (!executor.awaitTermination(SHUTDOWN_TIME, TimeUnit.SECONDS)) {
            *    LOG.error("Executor did not terminate in the specified time.");
            *    List<Runnable> droppedTasks = executor.shutdownNow();
            *    LOG.error("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
            }*/

        }
    }
}
