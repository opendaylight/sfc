/**
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer.sfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.l2renderer.SfcL2AbstractDataListener;
import org.opendaylight.sfc.l2renderer.SfcL2FlowProgrammerInterface;
import org.opendaylight.sfc.l2renderer.SfcL2Utils;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAlgAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has will be notified when changes are mad to Service function group.
 * @author Shlomi Alfasi (shlomi.alfasi@contextream.com)
 * @version 0.1 <p/>
 * @since 2015-18-04
 */
public class SfcL2SfgDataListener extends SfcL2AbstractDataListener {

    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2SfgDataListener.class);

    public SfcL2SfgDataListener(DataBroker dataBroker, SfcL2FlowProgrammerInterface sfcL2FlowProgrammer) {
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.SFG_ENTRY_IID);
        registerAsDataChangeListener(LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        // SFG create
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject = change.getCreatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionGroup) {
                LOG.info("SfcL2SfgDataListener.onDataChanged Add SFG {}",
                        ((ServiceFunctionGroup) entry.getValue()).getName());
                ServiceFunctionGroup sfg = (ServiceFunctionGroup) entry.getValue();
                buildGroup(sfg, true);
            }
        }

        // SFG update
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionGroup && (!(dataCreatedConfigurationObject
                    .containsKey(entry.getKey()))))) {
                LOG.info("SfcL2SfgDataListener.onDataChanged Update SFG {}",
                        ((ServiceFunctionGroup) entry.getValue()).getName());
                ServiceFunctionGroup sfg = (ServiceFunctionGroup) entry.getValue();
                buildGroup(sfg, true);
            }
        }

        // SFG delete
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier<?> instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = change.getOriginalData().get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionGroup) {
                LOG.info("SfcL2SfgDataListener.onDataChanged remove SFG {}",
                        ((ServiceFunctionGroup) dataObject).getName());
                ServiceFunctionGroup sfg = (ServiceFunctionGroup) dataObject;
                buildGroup(sfg, false);
            }
        }
    }

    private void buildGroup(ServiceFunctionGroup sfg, boolean isAdd) {
        try {
            List<SfcServiceFunction> sfs = sfg.getSfcServiceFunction();
            ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfs.get(0).getName());
            // assuming all SF's have the same SFF
            // should use the ovs id
            String sffName = sf.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();
            String sffNodeId = null;
            sffNodeId = getSffOpenFlowNodeName(sffName);

            if(sffNodeId == null){
                LOG.warn("failed to find switch configuration: sffName: {}- \naborting", sffName);
                return;
            }

            ServiceFunctionGroupAlgorithm algorithm = SfcProviderServiceFunctionGroupAlgAPI
                    .readServiceFunctionGroupAlg(sfg.getAlgorithm());

            List<GroupBucketInfo> bucketsInfo = new ArrayList<GroupBucketInfo>();

            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sffName);


            int index = 0;
            for (SfcServiceFunction sfcServiceFunction : sfg.getSfcServiceFunction()) {
                sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfcServiceFunction.getName());
                ServiceFunctionDictionary sffSfDict = SfcL2Utils.getSffSfDictionary(sff, sfcServiceFunction.getName());
                String outPort = SfcL2Utils.getDictPortInfoPort(sffSfDict);
                bucketsInfo.add(buildBucket(sf, outPort, index));
                index++;
            }
            this.sfcL2FlowProgrammer.configureGroup(sffName, sffNodeId, sfg.getName(), sfg.getGroupId(), algorithm.getAlgorithmType().getIntValue(), bucketsInfo, isAdd);

        } catch (Exception e) {
            LOG.warn("Failed generating group " + sfg, e);
        }
    }

    private GroupBucketInfo buildBucket(ServiceFunction sf, String outPort, int index) {
        GroupBucketInfo gbi = new GroupBucketInfo();
        gbi.setIndex(index);

        SfDataPlaneLocator sfDpl = sf.getSfDataPlaneLocator().get(0);
        gbi.setSfMac(getSfDplMac(sfDpl));
        gbi.setSfIp(getSfDplIp(sfDpl));
        gbi.setOutPort(outPort);

        return gbi;
    }

    private String getSfDplMac(SfDataPlaneLocator sfDpl) {
        String sfMac = null;

        LocatorType sffLocatorType = sfDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        LOG.debug("implementedInterface: {}", implementedInterface);
        // Mac/IP and possibly VLAN
        if (implementedInterface.equals(Mac.class)) {
            if (((MacAddressLocator) sffLocatorType).getMac() != null) {
                sfMac = ((MacAddressLocator) sffLocatorType).getMac().getValue();
            }
        }

        return sfMac;
    }

    private String getSfDplIp(SfDataPlaneLocator sfDpl) {
        String sfIp = null;

        LocatorType sffLocatorType = sfDpl.getLocatorType();
        Class<? extends DataContainer> implementedInterface = sffLocatorType.getImplementedInterface();

        if (implementedInterface.equals(Ip.class)) {
            if (((IpPortLocator) sffLocatorType).getIp() != null) {
                sfIp = String.valueOf(((IpPortLocator) sffLocatorType).getIp().getValue());
            }
        }
        return sfIp;
    }

    private String getSffOpenFlowNodeName(final String sffName) {
        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sffName);
        return SfcL2Utils.getSffOpenFlowNodeName(sff);
     }
}