/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.rspupdatelistener;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassifierRspUpdateDataGetter {
    /**
     * @param theRspName the name of the RSP we want to filter
     * @return  a list of all the ACLs that apply to the given RSP name
     */
    public List<Acl> filterAclsByRspName(RspName theRspName) {
        InstanceIdentifier<AccessLists>
                ifConfigIID = InstanceIdentifier.builder(AccessLists.class).build();

        return Optional.ofNullable(SfcDataStoreAPI.readTransactionAPI(ifConfigIID, LogicalDatastoreType.CONFIGURATION))
                .map(AccessLists::getAcl)
                .orElse(Collections.emptyList())
                .stream()
                .filter(acl -> acl.getAccessListEntries() != null)
                .filter(acl -> acl.getAccessListEntries().getAce()
                        .stream()
                        .map(Ace::getActions)
                        .map(actions -> actions.getAugmentation(Actions1.class))
                        .map(actions1 -> (AclRenderedServicePath) actions1.getSfcAction())
                        .anyMatch(sfcAction -> sfcAction.getRenderedServicePath().equals(theRspName.getValue())))
                .collect(Collectors.toList());
    }

    /**
     * @param theAclName the name of the ACL we want to filter
     * @return  a list of all the {@link SclServiceFunctionForwarder} enforcing the given ACL
     */
    public List<SclServiceFunctionForwarder> filterClassifierNodesByAclName(String theAclName) {
        InstanceIdentifier<ServiceFunctionClassifiers>
                ifConfigIID = InstanceIdentifier.builder(ServiceFunctionClassifiers.class).build();

        return Optional.ofNullable(SfcDataStoreAPI.readTransactionAPI(ifConfigIID, LogicalDatastoreType.CONFIGURATION))
                .map(ServiceFunctionClassifiers::getServiceFunctionClassifier)
                .orElse(Collections.emptyList())
                .stream()
                .filter(classifier -> classifier.getAcl().getName().equals(theAclName))
                .map(ServiceFunctionClassifier::getSclServiceFunctionForwarder)
                .findAny()
                .orElse(Collections.emptyList());
    }
}
