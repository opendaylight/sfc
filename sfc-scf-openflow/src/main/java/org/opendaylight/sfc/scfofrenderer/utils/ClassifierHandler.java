/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer.utils;

import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;

import java.util.Optional;

public class ClassifierHandler {
    /**
     * Build the name of the FlowKey given the names of the classifier, ACL, ACE, and the type of flow.
     * @param scfName   the name of the classifier
     * @param aclName   the name of the ACL
     * @param aceName   the name of the ACE
     * @param type      the type of flow. The possible types are: 'in', 'out', and 'relay'
     * @return          the name which will be given to the flow object
     */
    public String buildFlowKeyName(String scfName, String aclName, String aceName, String type) {
        return scfName + aclName + aceName + type;
    }

    /**
     * Get the name of the interface we want to classify.
     * @param theClassifier the classifier from which we want the InterfaceName
     * @return              the InterfaceName as a String, if present
     */
    public Optional<String> getInterfaceNameFromClassifier(SclServiceFunctionForwarder theClassifier) {
        return Optional.ofNullable(theClassifier)
                .filter(classifier -> classifier.getAttachmentPointType() instanceof Interface)
                .map(classifier -> (Interface) classifier.getAttachmentPointType())
                .map(Interface::getInterface);
    }

    /**
     * @param nodeName  the node from which the flow will be deleted
     * @param flowKey   the key of the flow we want to delete
     * @param tableID   the table from which we want to delete the flow
     * @return          the {@link FlowDetails} object that identifies the desired flow
     */
    public FlowDetails deleteFlowFromTable(String nodeName, String flowKey, short tableID) {
        return new FlowDetails(nodeName, new FlowKey(new FlowId(flowKey)), new TableKey(tableID));
    }

    /**
     * @param nodeName  the node in which the flow will be installed
     * @param flow      the flow to install
     * @param rspId     the RSP path ID to which the flow belongs to
     * @return          the {@link FlowDetails} object that identifies the desired flow
     */
    public FlowDetails addRspRelatedFlowIntoNode(String nodeName, FlowBuilder flow, long rspId) {
        return new FlowDetails(nodeName, flow.getKey(), new TableKey(flow.getTableId()), flow.build(), rspId);
    }

    /**
     * @param theSff    the {@link ServiceFunctionForwarder} to which the classifier is connected to
     * @return          true if theSff utilizes logical interfaces, false otherwise
     */
    public boolean usesLogicalInterfaces(ServiceFunctionForwarder theSff) {
        return theSff.getSffDataPlaneLocator() == null;
    }

    /**
     * @param scf   the SCF classifier object
     * @return      the ACL object, if found
     */
    public Optional<Acl> extractAcl(ServiceFunctionClassifier scf) {
        return Optional.ofNullable(scf)
                .map(ServiceFunctionClassifier::getAcl)
                .map(acl -> SfcProviderAclAPI.readAccessList(acl.getName(), acl.getType()));
    }
}
