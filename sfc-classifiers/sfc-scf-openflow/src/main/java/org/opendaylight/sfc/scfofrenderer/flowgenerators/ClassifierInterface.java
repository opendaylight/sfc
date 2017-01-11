/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;

import java.util.List;
import java.util.Optional;

public interface ClassifierInterface {
    FlowDetails initClassifierTable(String classifierNodeName);

    FlowDetails createClassifierOutFlow(String flowKey, Match match, SfcNshHeader sfcNshHeader, String classifierNodeName);

    FlowDetails createClassifierInFlow(String flowKey, SfcNshHeader sfcNshHeader, Long outPort, String nodeName);

    FlowDetails createClassifierRelayFlow(String flowKey, SfcNshHeader sfcNshHeader, String nodeName);

    List<FlowDetails> createDpdkFlows(String nodeName, long rspPathId);

    Optional<String> getNodeName(String theInterfaceName);

    Optional<Long> getInPort(String ifName, String nodeName);
}
