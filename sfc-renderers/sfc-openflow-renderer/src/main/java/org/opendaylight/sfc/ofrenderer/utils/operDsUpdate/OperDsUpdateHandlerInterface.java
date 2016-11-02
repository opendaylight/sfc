/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.utils.operDsUpdate;

import org.opendaylight.sfc.ofrenderer.processors.SffGraph;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;

/**
 * Interface for processors which expose state in the operational datastore
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 */
public interface OperDsUpdateHandlerInterface {

    /**
     * This method is invoked by the renderer in order to request the
     * update of the operational datastore after the successful creation
     * of a RSP
     * @param theGraph {@link SffGraph} used during RSP rendering
     * @param rsp  The rendered service path
     */
    public void onRspCreation(SffGraph theGraph, RenderedServicePath rsp);

    /**
     * This method is invoked by the renderer in order to request the
     * update of the operational datastore after the successful deletion
     * of a RSP
     * @param rsp  The just-deleted rendered service path
     */
    public void onRspDeletion(RenderedServicePath rsp);

}
