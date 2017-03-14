/*
 * Copyright (c) 2017 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.openflow;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.sfc.ofrenderer.sfg.GroupBucketInfo;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;

public class SfcSfgFlowProgrammerImpl extends SfcFlowProgrammerBase {

    public SfcSfgFlowProgrammerImpl(SfcOfFlowWriterInterface sfcOfFlowWriter, SfcOpenFlowConfig openFlowConfig) {
        super(sfcOfFlowWriter, openFlowConfig);
    }

    public void configureGroup(String sffNodeName, String openflowNodeId, String sfgName, long sfgId, int groupType,
            List<GroupBucketInfo> bucketInfos, boolean isAddGroup) {

        LOG.debug("configuring group: sffName {}, groupName {}, ofNodeId {}, id {}, type {}", sffNodeName, sfgName,
                openflowNodeId, sfgId, groupType);
        GroupBuilder gb = new GroupBuilder();
        gb.setBarrier(true);
        gb.setGroupType(GroupTypes.forValue(groupType));
        gb.setGroupName(sfgName);
        gb.setGroupId(new GroupId(sfgId));

        List<Bucket> buckets = new ArrayList<>();
        BucketBuilder bb = new BucketBuilder();
        for (GroupBucketInfo bucketInfo : bucketInfos) {
            LOG.debug("building bucket {}", bucketInfo);
            buckets.add(buildBucket(bb, bucketInfo));
        }
        BucketsBuilder bbs = new BucketsBuilder();
        bbs.setBucket(buckets);
        gb.setBuckets(bbs.build());
        String nodeName = openflowNodeId != null ? openflowNodeId : sffNodeName;
        sfcOfFlowWriter.writeGroupToDataStore(nodeName, gb, isAddGroup);
    }

    private Bucket buildBucket(BucketBuilder bb, GroupBucketInfo bucketInfo) {
        int order = 0;
        BucketId bucketId = new BucketId((long) bucketInfo.getIndex());
        bb.setBucketId(bucketId);
        bb.setKey(new BucketKey(bucketId));
        String sfMac = bucketInfo.getSfMac();
        String sfIp = bucketInfo.getSfIp();
        List<Action> actionList = new ArrayList<>();
        if (sfMac != null) {
            // Set the DL (Data Link) Dest Mac Address
            Action actionDstMac = SfcOpenflowUtils.createActionSetDlDst(sfMac, order);
            order++;
            actionList.add(actionDstMac);
        }

        if (sfIp != null) {
            Action actionSetNwDst = SfcOpenflowUtils.createActionSetNwDst(sfIp, 32, order);
            order++;
            actionList.add(actionSetNwDst);
        }
        Action actionOutPort = SfcOpenflowUtils.createActionOutPort(bucketInfo.getOutPort(), order);
        actionList.add(actionOutPort);

        bb.setAction(actionList);
        return bb.build();
    }

    public void configureGroupNextHopFlow(String sffNodeName, long sfpId, String srcMac, long groupId,
            String groupName) {
        LOG.debug("SfcProviderSffFlowWriter.ConfigureGroupNextHopFlow sffName [{}] sfpId [{}] srcMac [{}] groupId[{}]",
                sffNodeName, sfpId, srcMac, groupId);

        int flowPriority = FLOW_PRIORITY_NEXT_HOP;

        //
        // Create the matching criteria
        MatchBuilder match = new MatchBuilder();

        // Match on the either the metadata sfpId or the NSH NSP and NSI
        SfcOpenflowUtils.addMatchMetada(match, getMetadataSFP(sfpId), METADATA_MASK_SFP_MATCH);

        // match on the src mac
        if (srcMac != null) {
            SfcOpenflowUtils.addMatchSrcMac(match, srcMac);
        } else {
            // If the srcMac is null, then the packet is entering SFC and we
            // dont know
            // from where. Make it a lower priority, and only match on the
            // pathId
            flowPriority -= 10;
        }

        GroupActionBuilder groupActionBuilder = new GroupActionBuilder();
        groupActionBuilder.setGroupId(groupId);
        groupActionBuilder.setGroup(groupName);

        GroupActionCaseBuilder groupActionCaseBuilder = new GroupActionCaseBuilder();
        groupActionCaseBuilder.setGroupAction(groupActionBuilder.build());

        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setAction(groupActionCaseBuilder.build());
        actionBuilder.setOrder(0);
        actionBuilder.setKey(new ActionKey(0));
        Action groupAction = actionBuilder.build();

        // Create the Actions
        List<Action> actionList = new ArrayList<>();
        actionList.add(groupAction);

        // Create an Apply Action
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);

        // Create and configure the FlowBuilder
        FlowBuilder nextHopFlow = SfcOpenflowUtils.createFlowBuilder(getTableId(TABLE_INDEX_NEXT_HOP), flowPriority,
                NEXT_HOP_FLOW_NAME_LITERAL, match, isb);
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, nextHopFlow);
    }

}
