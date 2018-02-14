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
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcRspInfo;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.util.macchaining.SfcModelUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.hpe.params.xml.ns.yang.sfc.sff.termination.rev170111.SffDplChainTerminationAugment;
import org.opendaylight.yang.gen.v1.urn.hpe.params.xml.ns.yang.sfc.sff.termination.rev170111.termination.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MacChainingClassifier implements ClassifierInterface {
    private ServiceFunctionForwarder classifierSff;

    private final ClassifierHandler classifierHandler;

    private static final Logger LOG = LoggerFactory.getLogger(MacChainingClassifier.class);

    public MacChainingClassifier() {
        classifierHandler = new ClassifierHandler();
    }

    public MacChainingClassifier(ServiceFunctionForwarder theSff) {
        this();
        classifierSff = theSff;
    }

    public MacChainingClassifier setSff(ServiceFunctionForwarder theSff) {
        classifierSff = theSff;
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
        SffName classifierName = classifierSff.getName();

        SffDataPlaneLocator outputDpl = SfcModelUtil.searchSrcDplInConnectedSffs(classifierName, firstSffName);
        if (outputDpl == null) {
            LOG.error("Could not get source locator that connects SFFs {} and {}", classifierName, firstSffName);
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = outputDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            LOG.error("There is no augmentation with port details available for locator {}", outputDpl);
            return null;
        }

        FlowBuilder fb = SfcScfOfUtils.createMacChainClassifierOutFlow(
                nodeId, String.format("%s.%s", flowKey, ofsDpl.getOfsPort().getPortId()), match,
                        ofsDpl.getOfsPort().getPortId(), sfcRspInfo.getNshNsp(), sfcRspInfo.getNshStartNsi());

        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, fb, sfcRspInfo.getNshNsp());

    }

    @Override
    public FlowDetails createClassifierInFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo, Long outPort) {

        List<SffDataPlaneLocator> sffDataPlaneLocatorList = classifierSff.getSffDataPlaneLocator();

        TerminationPoint terminationPoint = null;
        for (SffDataPlaneLocator dpl : sffDataPlaneLocatorList) {
            SffDplChainTerminationAugment terminationDpl = dpl.getAugmentation(SffDplChainTerminationAugment.class);
            if (terminationDpl != null) {
                terminationPoint = terminationDpl.getTerminationPoint();
                break;
            }
        }
        if (terminationPoint == null) {
            LOG.error("There is no chain termination point specified in any locator of classifier SFF {}",
                    classifierSff.getName());
            return null;
        }

        String classifierNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(classifierSff);
        if (classifierNodeName == null) {
            LOG.error("Could not find the openflow node for classifier SFF {}", classifierSff.getName());
            return null;
        }

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
    public FlowDetails createClassifierRelayFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo) {

        SffName lastSff = sfcRspInfo.getLastSffName();
        SffName classifierName = classifierSff.getName();

        SffDataPlaneLocator returnSffDpl = SfcModelUtil.searchSrcDplInConnectedSffs(lastSff, classifierName);
        if (returnSffDpl == null) {
            LOG.error("Could not get source locator that connects SFFs {} and {}", lastSff, classifierName);
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = returnSffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            LOG.error("There is no augmentation with port details available for locator {}", returnSffDpl);
            return null;
        }

        FlowBuilder fb = SfcScfOfUtils.createClassifierMacChainingRelayFlow(
                nodeId, flowKey, ofsDpl.getOfsPort().getPortId(), sfcRspInfo.getNshNsp(),
                sfcRspInfo.getNshStartNsi(), sfcRspInfo.getNshEndNsi());

        return classifierHandler.addRspRelatedFlowIntoNode(nodeId, fb, sfcRspInfo.getNshNsp());

    }

    @Override
    public Optional<String> getNodeName(String interfaceName) {
        return Optional.ofNullable(classifierSff).map(SfcOvsUtil::getOpenFlowNodeIdForSff);
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
