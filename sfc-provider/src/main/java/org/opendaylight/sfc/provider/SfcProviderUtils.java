/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
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

public class SfcProviderUtils {
    public static final long SHUTDOWN_TIME = 5;
    public static final String THREAD_FACTORY_NAME_FORMAT = "SFC-%d";
    public static final boolean THREAD_FACTORY_IS_DAEMON = false;
    public static final int EXECUTOR_THREAD_POOL_SIZE = 100;

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

    private static DataBroker dataBroker;
    private static BindingAwareBroker bindingAwareBroker;
    private static SfcProviderUtils instance;
    private static ExecutorService executor;

    public SfcProviderUtils(final DataBroker dataBroker, final BindingAwareBroker broker, final ExecutorService executor) {
        SfcProviderUtils.dataBroker = dataBroker;
        SfcProviderUtils.bindingAwareBroker = broker;
        SfcProviderUtils.executor = executor;
        if (instance == null) {
            instance = this;
        }
    }

    public BindingAwareBroker getBroker() {
        return bindingAwareBroker;
    }

    public DataBroker getDataProvider() {
        return dataBroker;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public static SfcProviderUtils getSfcProviderUtils() {
        return instance;
    }
}
