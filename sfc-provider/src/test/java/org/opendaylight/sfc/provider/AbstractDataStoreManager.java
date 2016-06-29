/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.AccessListsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.ServiceFunctionState1;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import static com.google.common.base.Preconditions.checkState;

/**
 * This class contains auxiliary methods to manage abstract data store
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-07-22
 */

/*
 * The purpose of this class is to manage abstract data store used in tests
 */
public abstract class AbstractDataStoreManager extends AbstractDataBrokerTest {

    protected DataBroker dataBroker;
    protected static final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();
    protected static ExecutorService executor = opendaylightSfc.getExecutor();

    // initial sfc odl setup, executor is set only once, new data broker is created before every
    // set, it ensures empty data store
    protected void setOdlSfc() {
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
    }

    /*
     * loads only SFC YANG modules - increased performance
     * Specify a class from YANG which should be loaded
     */
    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        Builder<YangModuleInfo> moduleInfoSet = ImmutableSet.<YangModuleInfo>builder();
        loadModuleInfos(ServiceFunction.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionForwarder.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionChain.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionPath.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionGroup.class, moduleInfoSet);
        loadModuleInfos(ServicePathId.class, moduleInfoSet);
        loadModuleInfos(RenderedServicePath.class, moduleInfoSet);
        loadModuleInfos(AccessLists.class, moduleInfoSet);
        loadModuleInfos(AccessListsState.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionClassifiers.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionState.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionState1.class, moduleInfoSet);
        loadModuleInfos(ServiceFunctionTypes.class, moduleInfoSet);
        return moduleInfoSet.build();
    }

    public static void loadModuleInfos(Class<?> clazzFromModule, Builder<YangModuleInfo> moduleInfoSet)
            throws Exception {
        YangModuleInfo moduleInfo = BindingReflections.getModuleInfo(clazzFromModule);
        checkState(moduleInfo != null, "Module Info for %s is not available.", clazzFromModule);
        collectYangModuleInfo(moduleInfo, moduleInfoSet);
    }

    private static void collectYangModuleInfo(final YangModuleInfo moduleInfo,
            final Builder<YangModuleInfo> moduleInfoSet) throws IOException {
        moduleInfoSet.add(moduleInfo);
        for (YangModuleInfo dependency : moduleInfo.getImportedModules()) {
            collectYangModuleInfo(dependency, moduleInfoSet);
        }
    }
}
