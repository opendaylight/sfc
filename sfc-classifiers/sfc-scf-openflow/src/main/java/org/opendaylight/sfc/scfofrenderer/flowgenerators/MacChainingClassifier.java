/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcRspInfo;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.util.macchaining.SfcModelUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.hpe.params.xml.ns.yang.sfc.sff.termination.rev170111.SffDplChainTerminationAugment;
import org.opendaylight.yang.gen.v1.urn.hpe.params.xml.ns.yang.sfc.sff.termination.rev170111.termination.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;



public class MacChainingClassifier implements ClassifierInterface {
    private ServiceFunctionForwarder serviceFunctionForwarder;

    private final ClassifierHandler classifierHandler;

    public MacChainingClassifier() {
        classifierHandler = new ClassifierHandler();
    }

    public MacChainingClassifier(ServiceFunctionForwarder theSff) {
        this();
        serviceFunctionForwarder = theSff;
    }

    public MacChainingClassifier setSff(ServiceFunctionForwarder theSff) {
        serviceFunctionForwarder = theSff;
        return this;
    }

    @Override
    public FlowDetails initClassifierTable(String nodeId) {
        return classifierHandler.addRspRelatedFlowIntoNode(nodeId,
                SfcScfOfUtils.initClassifierTable(),
                OpenflowConstants.SFC_FLOWS);
    }

    @Override
    public FlowDetails createClassifierOutFlow(String nodeId, String flowKey, Match match, SfcRspInfo sfcRspInfo) {

        SffName firstSffName = sfcRspInfo.getFirstSffName();

        SffName classifier = new SffName(SfcProviderServiceForwarderAPI.getSffName(nodeId));

        SffDataPlaneLocator outputDpl = SfcModelUtil.searchSrcDplInConnectedSffs(classifier, firstSffName);

        if (outputDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = outputDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            return null;
        }

        FlowBuilder fb = SfcScfOfUtils.createMacChainClassifierOutFlow(
                nodeId, String.format("%s.%s", flowKey, ofsDpl.getOfsPort().getPortId()), match,
                        ofsDpl.getOfsPort().getPortId(), sfcRspInfo.getNshNsp(), sfcRspInfo.getNshStartNsi());

        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, fb, sfcRspInfo.getNshNsp());

    }

    @Override
    public FlowDetails createClassifierInFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo, Long outPort) {

        SffName classifier = new SffName(SfcProviderServiceForwarderAPI.getSffName(nodeId));
        ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(classifier);

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = sff.getSffDataPlaneLocator();

        TerminationPoint terminationPoint = null;
        for (SffDataPlaneLocator dpl : sffDataPlaneLocatorList) {
            SffDplChainTerminationAugment terminationDpl = dpl.getAugmentation(SffDplChainTerminationAugment.class);
            if (terminationDpl != null) {
                terminationPoint = terminationDpl.getTerminationPoint();
                break;
            }
        }
        if (terminationPoint == null) {
            return null;
        }

        String classifierNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(classifier));

        FlowBuilder fb = SfcScfOfUtils.createMacChainClassifierInFlow(
                classifierNodeName, String.format("%s.%s", flowKey, sfcRspInfo.getNshNsp().toString()),
                terminationPoint.getPortId(), terminationPoint.getMacAddress().getValue(),
                sfcRspInfo.getNshNsp(), sfcRspInfo.getNshStartNsi());

        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName, fb,sfcRspInfo.getNshNsp());

    }

    @Override
    public List<FlowDetails> createDpdkFlows(String nodeId, SfcRspInfo sfcRspInfo) {
        return Collections.emptyList();
    }


    @Override
    public FlowDetails createClassifierRelayFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo,
                                                 String classifierName) {

        SffName lastSff = sfcRspInfo.getLastSffName();

        SffName classifierSff = new SffName(classifierName);

        SffDataPlaneLocator returnSffDpl = SfcModelUtil.searchSrcDplInConnectedSffs(lastSff, classifierSff);

        if (returnSffDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = returnSffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            return null;
        }

        FlowBuilder fb = SfcScfOfUtils.createClassifierMacChainingRelayFlow(
                nodeId, flowKey, ofsDpl.getOfsPort().getPortId(), sfcRspInfo.getNshNsp(),
                sfcRspInfo.getNshStartNsi(), sfcRspInfo.getNshEndNsi());

        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, fb, sfcRspInfo.getNshNsp());

    }

    @Override
    public Optional<String> getNodeName(String interfaceName) {
        return Optional.ofNullable(serviceFunctionForwarder)
                .filter(theSff -> theSff.getAugmentation(SffOvsBridgeAugmentation.class) != null)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);
    }


    @Override
    public Optional<Long> getInPort(String nodeId, String interfaceName) {
        return Optional.ofNullable(SfcOvsUtil.getOfPortByName(nodeId, interfaceName));
    }

    @Override
    public short getClassifierTable() {
        return SfcScfOfUtils.TABLE_INDEX_CLASSIFIER;
    }

    @Override
    public short getTransportIngressTable() {
        return SfcScfOfUtils.TABLE_INDEX_INGRESS_TRANSPORT;
    }

}
