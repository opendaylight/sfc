/**
 * Copyright (c) 2018 Inocybe Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.statistics.testutils;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfTableOffsets;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public class AbstractDataStoreManager extends AbstractConcurrentDataBrokerTest {
    protected DataBroker dataBroker;
    protected SfcInstanceIdentifiers sfcIids;
    protected static ExecutorService executor = Executors.newFixedThreadPool(5);

    // initial sfc setup, executor is set only once, new data broker
    // is created before every set, it ensures empty data store
    protected void setupSfc() {
        dataBroker = getDataBroker();
        SfcDataStoreAPI.setDataProviderAux(dataBroker);
        sfcIids = new SfcInstanceIdentifiers();
    }

    protected void close() throws ExecutionException, InterruptedException {
        if (sfcIids != null) {
            // Deletes everything from SFC that was created in the datastore
            sfcIids.close();
        }
    }

    /*
     * loads only SFC YANG modules - increased performance Specify a class from
     * YANG which should be loaded
     */
    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        ImmutableSet.Builder<YangModuleInfo> moduleInfoSet = ImmutableSet.<YangModuleInfo>builder();
        loadModuleInfos(Nodes.class, moduleInfoSet);
        loadModuleInfos(FlowCapableNode.class, moduleInfoSet);
        loadModuleInfos(FlowStatisticsData.class, moduleInfoSet);
        loadModuleInfos(ServiceFunction.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionForwarder.class, moduleInfoSet);
        loadModuleInfos(SffOvsBridgeAugmentationBuilder.class, moduleInfoSet);
        loadModuleInfos(SfcOfTableOffsets.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionChain.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionPath.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionGroup.class, moduleInfoSet);
        loadModuleInfos(ServicePathId.class, moduleInfoSet);
        loadModuleInfos(RenderedServicePath.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionState.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionState1.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionTypes.class, moduleInfoSet);
        return moduleInfoSet.build();
    }

    public static void loadModuleInfos(Class<?> clazzFromModule, ImmutableSet.Builder<YangModuleInfo> moduleInfoSet)
            throws Exception {
        YangModuleInfo moduleInfo = BindingReflections.getModuleInfo(clazzFromModule);
        checkState(moduleInfo != null, "Module Info for %s is not available.", clazzFromModule);
        collectYangModuleInfo(moduleInfo, moduleInfoSet);
    }

    private static void collectYangModuleInfo(final YangModuleInfo moduleInfo,
            final ImmutableSet.Builder<YangModuleInfo> moduleInfoSet) throws IOException {
        moduleInfoSet.add(moduleInfo);
        for (YangModuleInfo dependency : moduleInfo.getImportedModules()) {
            collectYangModuleInfo(dependency, moduleInfoSet);
        }
    }
}
