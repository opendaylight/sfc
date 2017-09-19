/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import org.opendaylight.sfc.ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.util.macchaining.SfcModelUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.hpe.params.xml.ns.yang.sfc.sff.termination.rev170111.SffDplChainTerminationAugment;
import org.opendaylight.yang.gen.v1.urn.hpe.params.xml.ns.yang.sfc.sff.termination.rev170111.termination.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;



public class MacChainingClassifier {
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

    public FlowDetails initClassifierTable(String classifierNodeName) {
        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName,
                SfcScfOfUtils.initClassifierTable(),
                OpenflowConstants.SFC_FLOWS);
    }


    public FlowDetails createClassifierOutFlow(
            String flowKey, Match match, RspName rspName, String classifierNodeName) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);


        List<RenderedServicePathHop> renderedServicePathHopList = rsp.getRenderedServicePathHop();
        RenderedServicePathHop renderedServicePathHop = renderedServicePathHopList.get(0);
        SffName firstSffName = renderedServicePathHop.getServiceFunctionForwarder();

        SffName classifier = new SffName(SfcProviderServiceForwarderAPI.getSffName(classifierNodeName));

        SffDataPlaneLocator outputDpl = SfcModelUtil.searchSrcDplInConnectedSffs(classifier, firstSffName);

        if (outputDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = outputDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            return null;
        }

        String nodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(classifier));

        FlowBuilder fb = SfcScfOfUtils.createMacChainClassifierOutFlow(
                nodeName, String.format("%s.%s", flowKey, ofsDpl.getOfsPort().getPortId()), match,
                        ofsDpl.getOfsPort().getPortId(), rsp.getPathId(), rsp.getStartingIndex());

        return classifierHandler.addRspRelatedFlowIntoNode(nodeName,
                fb, rsp.getPathId());

    }


    public FlowDetails createClassifierInFlow(String flowKey, RspName rspName, Long outPort, String nodeName) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);

        SffName classifier = new SffName(SfcProviderServiceForwarderAPI.getSffName(nodeName));
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
                classifierNodeName, String.format("%s.%s", flowKey, rsp.getPathId().toString()),
                        terminationPoint.getPortId(), terminationPoint.getMacAddress().getValue(),
                        rsp.getPathId(), rsp.getStartingIndex());

        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName,
                fb, rsp.getPathId());

    }


    public FlowDetails createClassifierRelayFlow(String flowKey, RspName rspName, String nodeName, String classifier) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);

        RenderedServicePathHop lastRspHop = Iterables.getLast(rsp.getRenderedServicePathHop());
        SffName lastSff = lastRspHop.getServiceFunctionForwarder();

        SffName classifierSff = new SffName(classifier);

        SffDataPlaneLocator returnSffDpl = SfcModelUtil.searchSrcDplInConnectedSffs(lastSff, classifierSff);

        if (returnSffDpl == null) {
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = returnSffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            return null;
        }
        String lastFFNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSff));

        FlowBuilder fb = SfcScfOfUtils.createClassifierMacChainingRelayFlow(
                lastFFNodeName, flowKey, ofsDpl.getOfsPort().getPortId(), rsp.getPathId(), rsp.getStartingIndex(),
                        (short) (lastRspHop.getServiceIndex().intValue() - 1));

        return classifierHandler.addRspRelatedFlowIntoNode(lastFFNodeName,
                fb, rsp.getPathId());

    }



    public Optional<String> getNodeName(String theInterfaceName) {
        return Optional.ofNullable(serviceFunctionForwarder)
                .filter(theSff -> theSff.getAugmentation(SffOvsBridgeAugmentation.class) != null)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);
    }


    public Optional<Long> getInPort(String ifName, String nodeName) {
        return Optional.ofNullable(SfcOvsUtil.getOfPortByName(nodeName, ifName));
    }

}
