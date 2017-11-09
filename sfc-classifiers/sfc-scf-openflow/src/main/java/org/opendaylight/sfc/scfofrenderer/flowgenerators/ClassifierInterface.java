/*
 * Copyright (c) 2016, 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import java.util.List;
import java.util.Optional;
import org.opendaylight.sfc.scfofrenderer.utils.SfcRspInfo;
import org.opendaylight.sfc.util.openflow.writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;

public interface ClassifierInterface {

    /**
     * Create classifier initialization flow.
     *
     * @param nodeId
     *            the node ID of the classifier (ex: "openflow:dpnID")
     * @return a FlowDetails object containing the desired flow
     */
    FlowDetails initClassifierTable(String nodeId);

    /**
     * Create classifier out (classifier -> SFF) flows.
     *
     * @param nodeId
     *            the node ID of the classifier (ex: "openflow:dpnID")
     * @param flowKey
     *            the key for the flow objects
     * @param match
     *            the Match object
     * @param sfcRspInfo
     *            the RSP info
     * @return a FlowDetails object containing the desired flow
     */
    FlowDetails createClassifierOutFlow(String nodeId, String flowKey, Match match, SfcRspInfo sfcRspInfo);

    /**
     * Create classifier in (SFF -> classifier) flow.
     *
     * @param nodeId
     *            the node ID of the classifier (ex: "openflow:dpnID")
     * @param flowKey
     *            the key for the flow objects
     * @param sfcRspInfo
     *            the RSP info
     * @param outPort
     *            the flow out port
     * @return a FlowDetails object containing the desired flow
     */
    FlowDetails createClassifierInFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo, Long outPort);

    /**
     * Create classifier relay flow.
     *
     * @param nodeId
     *            the ID of the relay node (ex: "openflow:dpnID")
     * @param flowKey
     *            the key for the flow objects
     * @param sfcRspInfo
     *            the RSP info
     * @param classifierName
     *            the node name of the classifier
     * @return a FlowDetails object containing the desired flow
     */
    FlowDetails createClassifierRelayFlow(String nodeId, String flowKey, SfcRspInfo sfcRspInfo, String classifierName);

    /**
     * Create DPDK flows.
     *
     * @param nodeId
     *            the node ID of the classifier (ex: "openflow:dpnID")
     * @param sfcRspInfo
     *            the RSP info
     * @return a list of FlowDetails object containing the desired flows
     */
    List<FlowDetails> createDpdkFlows(String nodeId, SfcRspInfo sfcRspInfo);

    /**
     * Get the name of the node connected to the supplied interface.
     *
     * @param interfaceName
     *            the interface name.
     * @return the name of the compute node hosting the supplied SF. ex:
     *         "openflow:xxx"
     */
    Optional<String> getNodeName(String interfaceName);

    /**
     * Get the input openflow port, given an interface name, and a nodeName, if
     * any.
     *
     * @param nodeId
     *            the ID of the node (ex: "openflow:dpnID")
     * @param interfaceName
     *            the name of the interface
     * @return the openflow port, if any
     */
    Optional<Long> getInPort(String nodeId, String interfaceName);

    /**
     * Get the number of the openflow table used by the SFC classifier.
     *
     * @return the number of the openflow table used by the SFC classifier
     */
    short getClassifierTable();

    /**
     * Get the number of the openflow table used by SFC transport ingress from
     * genius.
     *
     * @return the number of the openflow table used by the SFC transport
     *         ingress table
     */
    short getTransportIngressTable();
}
