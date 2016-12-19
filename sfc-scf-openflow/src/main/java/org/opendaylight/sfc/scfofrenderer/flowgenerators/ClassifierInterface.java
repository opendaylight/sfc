/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.flowgenerators;

import org.opendaylight.sfc.scfofrenderer.utils.SfcNshHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;

import java.util.Optional;

public interface ClassifierInterface {
    FlowBuilder initClassifierTable();

    FlowBuilder createClassifierOutFlow(String flowKey,
                                        Match match,
                                        SfcNshHeader sfcNshHeader,
                                        String classifierNodeName);

    FlowBuilder createClassifierInFlow(String flowKey,
                                       SfcNshHeader sfcNshHeader,
                                       Long outPort);

    FlowBuilder createClassifierRelayFlow(String flowKey, SfcNshHeader sfcNshHeader);

    Optional<String> getNodeName(String theInterfaceName);

    Optional<Long> getInPort(String ifName, String nodeName);
}
