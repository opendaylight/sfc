/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.rspupdatelistener;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;

import java.util.Collections;
import java.util.List;

public class ClassifierRspUpdateDataGetter {
    /**
     * @param theRspName the name of the RSP we want to filter
     * @return  a list of all the ACLs that apply to the given RSP name
     */
    public List<Acl> filterAclsByRspName(RspName theRspName) {
        // TODO
        return Collections.emptyList();
    }

    /**
     * @param theAclName the name of the ACL we want to filter
     * @return  a list of all the {@link SclServiceFunctionForwarder} enforcing the given ACL
     */
    public List<SclServiceFunctionForwarder> filterClassifierNodesByAclName(String theAclName) {
        // TODO
        return Collections.emptyList();
    }
}
