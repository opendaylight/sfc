/**
 * Copyright (c) 2014 by Ericsson and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.sfc.l2renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.l2renderer.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionGroupAlgAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.alg.rev150214.service.function.group.algorithms.ServiceFunctionGroupAlgorithm;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.MacAddressLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
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

    private enum ChangeType {
        ADD, //
        REMOVE, //
        UPDATE;
    }

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2SfgDataListener.class);

    public SfcL2SfgDataListener(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.SFG_ENTRY_IID);
        registerAsDataChangeListener(LogicalDatastoreType.CONFIGURATION);
        LOG.error("SfcL2SfgDataListener.constructor()");
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
                buildGroup(sfg, ChangeType.ADD);
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
                buildGroup(sfg, ChangeType.UPDATE);
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
                buildGroup(sfg, ChangeType.REMOVE);
            }
        }
    }

    private void buildGroup(ServiceFunctionGroup sfg, ChangeType changeType) {
        try {
            List<SfcServiceFunction> sfs = sfg.getSfcServiceFunction();
            ServiceFunction sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfs.get(0).getName());
            // assuming all SF's have the same SFF
            String sffName = sf.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();

            GroupBuilder gb = new GroupBuilder();
            BucketsBuilder bbs = new BucketsBuilder();
            gb.setBarrier(true);
            ServiceFunctionGroupAlgorithm algorithm = SfcProviderServiceFunctionGroupAlgAPI
                    .readServiceFunctionGroupAlg(sfg.getAlgorithm());
            gb.setGroupType(GroupTypes.forValue(algorithm.getAlgorithmType().getIntValue()));
            gb.setGroupName(sfg.getName());
            gb.setGroupId(new GroupId(sfg.getGroupId()));

            bbs.build();
            BucketBuilder bb = new BucketBuilder();
            List<Bucket> buckets = new ArrayList<Bucket>();
            int index = 0;
            for (SfcServiceFunction sfcServiceFunction : sfg.getSfcServiceFunction()) {
                sf = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sfcServiceFunction.getName());
                buckets.add(buildBucket(bb, sf, index));
                index++;
            }
            bbs.setBucket(buckets);
            gb.setBuckets(bbs.build());
            writeToDataStore(sffName, gb, changeType);
        } catch (Exception e) {
            LOG.warn("Failed generating group " + sfg, e);
        }
    }

    private Bucket buildBucket(BucketBuilder bb, ServiceFunction sf, int i) {
        BucketId bucketId = new BucketId((long) i);
        bb.setBucketId(bucketId);
        bb.setKey(new BucketKey(bucketId));
        SfDataPlaneLocator sfDpl = sf.getSfDataPlaneLocator().get(0);
        String sfMac = getSfDplMac(sfDpl);
        String sfIp = getSfDplIp(sfDpl);
        List<Action> actionList = new ArrayList<Action>();
        if (sfMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(sfMac, 0);
            actionList.add(actionDstMac);
        }

        if (sfIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(sfIp, 32, 0);
            actionList.add(actionSetNwDst);
        }

        bb.setAction(actionList);
        return bb.build();
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

    private void writeToDataStore(String sffNodeName, GroupBuilder gb, ChangeType changeType ) {
        // Create the NodeBuilder
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(sffNodeName));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        GroupKey gk = new GroupKey(gb.getGroupId());
        InstanceIdentifier<Group> groupIID;

        groupIID = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class, gk).build();
        Group group = gb.build();
        LOG.debug("writing group to data store \nID: {}\nGroup: {}", groupIID, group);
        if (changeType == ChangeType.ADD) {
            if (!SfcDataStoreAPI.writePutTransactionAPI(groupIID, group, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to write group to data store");
            }
        } else if (changeType == ChangeType.UPDATE) {
            if (!SfcDataStoreAPI.writeMergeTransactionAPI(groupIID, group, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to write group to data store");
            }
        } else if (changeType == ChangeType.REMOVE) {
            if (!SfcDataStoreAPI.deleteTransactionAPI(groupIID, LogicalDatastoreType.CONFIGURATION)) {
                LOG.warn("Failed to remove group from data store");
            }
        }
    }
}