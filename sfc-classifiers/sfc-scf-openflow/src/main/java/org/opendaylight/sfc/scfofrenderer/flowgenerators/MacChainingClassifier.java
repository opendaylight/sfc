/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import com.google.common.collect.Iterables;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.scfofrenderer.utils.ClassifierHandler;
import org.opendaylight.sfc.scfofrenderer.utils.SfcScfOfUtils;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.util.macChaining.SfcModelUtil;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
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
import java.util.List;
import java.util.Optional;


public class MacChainingClassifier {
    private ServiceFunctionForwarder sff;

    private final ClassifierHandler classifierHandler;

    public MacChainingClassifier() {classifierHandler = new ClassifierHandler();}

    public MacChainingClassifier(ServiceFunctionForwarder theSff) {
        this();
        sff = theSff;
    }

    public MacChainingClassifier setSff(ServiceFunctionForwarder theSff) {
        sff = theSff;
        return this;
    }

    public FlowDetails initClassifierTable(String classifierNodeName) {
        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName,
                SfcScfOfUtils.initClassifierTable(),
                OpenflowConstants.SFC_FLOWS);    }


    public FlowDetails createClassifierOutFlow(String flowKey, Match match, RspName rspName, String classifierNodeName) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);


        List<RenderedServicePathHop> renderedServicePathHopList = rsp.getRenderedServicePathHop();
        RenderedServicePathHop renderedServicePathHop = renderedServicePathHopList.get(0);
        SffName firstSffName = renderedServicePathHop.getServiceFunctionForwarder();

        SffName classifier = new SffName(SfcProviderServiceForwarderAPI.getSffName(classifierNodeName));

        //ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);

        SffDataPlaneLocator outputDpl = SfcModelUtil.serachSrcDplInConnectedSffs(classifier, firstSffName);

        if (outputDpl == null) {
            //LOG.error(" not find SFF dictionary in classifier {} to  {} ", classifier, firstSffName);
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = outputDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            //LOG.debug("No OFS DPL available for dpl [{}]", outputDpl.getName().getValue());
            return null;
        }

        String nodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(classifier));

        //new FlowDetails(nodeName, flowKey, new TableKey(flow.getTableId()), flow.build(), rspId)

        FlowBuilder fb = SfcScfOfUtils.createMacChainClassifierOutFlow
                (classifierNodeName, String.format("%s - %s", flowKey, ofsDpl.getOfsPort().getPortId()), match, ofsDpl.getOfsPort().getPortId(), rsp.getPathId(), rsp.getStartingIndex());

        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName,
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
            //LOG.error("No termination DPL available for dpl [{}]", sff.getName().toString());
            return null;
        }

        String classifierNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(classifier));

        FlowBuilder fb = SfcScfOfUtils.createMacChainClassifierInFlow
                (classifierNodeName, String.format("%s.%s", flowKey, rsp.getPathId().toString()), terminationPoint.getPortId(), terminationPoint.getMacAddress().getValue(), rsp.getPathId(), rsp.getStartingIndex());

        return classifierHandler.addRspRelatedFlowIntoNode(classifierNodeName,
                fb, rsp.getPathId());

    }


    public FlowDetails createClassifierRelayFlow(String flowKey, RspName rspName, String nodeName, String classifier) {

        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);

        RenderedServicePathHop lastRspHop = Iterables.getLast(rsp.getRenderedServicePathHop());
        SffName lastSff = lastRspHop.getServiceFunctionForwarder();

        SffName classifierSff = new SffName(classifier);


        SffDataPlaneLocator returnSffDpl = SfcModelUtil.serachSrcDplInConnectedSffs(lastSff, classifierSff);

        if (returnSffDpl == null) {
            //LOG.error(" not find SFF dictionary in SFF {} to  {} ", lastSff, classifier);
            return null;
        }

        SffDataPlaneLocator1 ofsDpl = returnSffDpl.getAugmentation(SffDataPlaneLocator1.class);
        if (ofsDpl == null) {
            //LOG.debug("No OFS DPL available for dpl [{}]", returnSffDpl.getName().getValue());
            return null;
        }
        String lastFFNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSff));

        FlowBuilder fb = SfcScfOfUtils.createClassifierMacChainingRelayFlow
                (nodeName, flowKey, ofsDpl.getOfsPort().getPortId(), rsp.getPathId(), rsp.getStartingIndex(), (short) (lastRspHop.getServiceIndex().intValue() - 1));

        return classifierHandler.addRspRelatedFlowIntoNode(lastFFNodeName,
                fb, rsp.getPathId());

    }



    public Optional<String> getNodeName(String theInterfaceName) {
        return Optional.ofNullable(sff)
                .filter(theSff -> theSff.getAugmentation(SffOvsBridgeAugmentation.class) != null)
                .map(SfcOvsUtil::getOpenFlowNodeIdForSff);    }


    public Optional<Long> getInPort(String ifName, String nodeName) {
        return Optional.ofNullable(SfcOvsUtil.getOfPortByName(nodeName, ifName));
    }

}
