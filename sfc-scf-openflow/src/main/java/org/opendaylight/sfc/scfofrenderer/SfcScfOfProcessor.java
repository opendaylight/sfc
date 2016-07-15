/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.scfofrenderer;


import java.util.List;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfProcessor.class);

    public SfcScfOfProcessor() {}

   /**
    * create flows for service function classifier
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  scf service function classifier
    * @return          create result
    */
    public boolean createdServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        if (scf == null) {
            LOG.error("createdServiceFunctionClassifier: scf is null\n");
            return false;
        }

        LOG.debug("create ServiceFunctionClassifier name: {} ACL: {} SFF: {}\n", scf.getName(), scf.getAcl(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAcl().getName(), scf.getAcl().getType());
        if (acl == null) {
            LOG.error("createdServiceFunctionClassifier: acl is null\n");
            return false;
        }

        String aclName = acl.getAclName();
        if (aclName == null) {
            LOG.error("createdServiceFunctionClassifier: aclName is null\n");
            return false;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.error("createdServiceFunctionClassifier: accessListEntries is null\n");
            return false;
        }

        List<Ace> acesList = accessListEntries.getAce();
        if (acesList == null) {
            LOG.error("createdServiceFunctionClassifier: acesList is null\n");
            return false;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("createdServiceFunctionClassifier: sfflist is null\n");
            return false;
        }

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            SffName sffName = new SffName(sclsff.getName());

            Long inPort = null;

            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff == null) {
                LOG.error("createdServiceFunctionClassifier: sff is null\n");
                continue;
            }

            SffOvsBridgeAugmentation ovsSff = sff.getAugmentation(SffOvsBridgeAugmentation.class);
            if (ovsSff == null) {
                LOG.debug("deletedServiceFunctionClassifier: sff is not ovs\n");
                continue;
            }

            String nodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(sff);
            if (nodeName == null) {
                LOG.error("createdServiceFunctionClassifier: nodeName is null\n");
                continue;
            }

            Long outPort = SfcOvsUtil.getVxlanOfPort(nodeName);
            SfcScfOfUtils.initClassifierTable(nodeName);

            if (sclsff.getAttachmentPointType() instanceof Interface) {
                Interface itf = (Interface) sclsff.getAttachmentPointType();
                if (itf == null) {
                    LOG.error("createdServiceFunctionClassifier: attachment point is null\n");
                    continue;
                }

                String itfName = itf.getInterface();
                if (itfName == null) {
                    LOG.error("createdServiceFunctionClassifier: interface is null\n");
                    continue;
                }
                inPort = SfcOvsUtil.getOfPortByName(nodeName, itfName);
                if (inPort == null) {
                    LOG.error("createdServiceFunctionClassifier: port is null\n");
                    continue;
                }
            }

            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                if (ruleName == null) {
                    LOG.error("createdServiceFunctionClassifier: ruleName is null\n");
                    continue;
                }

                StringBuffer sb = new StringBuffer();
                sb.append(nodeName).append(":");
                sb.append(String.valueOf(inPort));
                NodeConnectorId port = new NodeConnectorId(sb.toString());

                // Match
                Match match = new SfcScfMatch()
                                  .setPortMatch(port)
                                  .setAclMatch(ace.getMatches())
                                  .build();

                // Action
                Actions actions = ace.getActions();
                if (actions == null) {
                    LOG.error("createdServiceFunctionClassifier: action is null\n");
                    continue;
                }

                Actions1 a1 = actions.getAugmentation(Actions1.class);
                if (a1 == null) {
                    LOG.error("createdServiceFunctionClassifier: action augment is null\n");
                    continue;
                }

                AclRenderedServicePath path = (AclRenderedServicePath) a1.getSfcAction();
                if (path == null) {
                    LOG.error("createdServiceFunctionClassifier: sfc action is null\n");
                    continue;
                }

                RspName rspName = new RspName(path.getRenderedServicePath());
                SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName);


                if (nsh == null) {
                    LOG.error("createdServiceFunctionClassifier: nsh is null\n");
                    continue;
                }

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName).append(".out");
                if (!SfcScfOfUtils.createClassifierOutFlow(nodeName, key.toString(), match, nsh, outPort)) {
                    LOG.error("createdServiceFunctionClassifier: out flow is null\n");
                    continue;
                }

                RspName reverseRspName = null;
                if (path.getRenderedServicePath().endsWith("-Reverse")) {
                    reverseRspName = new RspName(path.getRenderedServicePath().replaceFirst("-Reverse", ""));
                } else {
                    reverseRspName = new RspName(path.getRenderedServicePath() + "-Reverse");
                }

                SfcNshHeader reverseNsh = SfcNshHeader.getSfcNshHeader(reverseRspName);

                if (reverseNsh == null) {
                    LOG.debug("createdServiceFunctionClassifier: reverseNsh is null\n");
                } else {
                    key = new StringBuffer();
                    key.append(scf.getName()).append(aclName).append(ruleName).append(".in");
                    if (!SfcScfOfUtils.createClassifierInFlow(nodeName, key.toString(), reverseNsh, inPort)) {
                        LOG.error("createdServiceFunctionClassifier: fail to create in flow\n");
                    }

                    SffName lastSffName = reverseNsh.getSffName();
                    if (lastSffName != null &&
                        !reverseNsh.getSffName().equals(sffName)) {
                        ServiceFunctionForwarder lastSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
                        String lastNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(lastSff);
                        if (lastNodeName == null) {
                            LOG.error("createdServiceFunctionClassifier: lastNodeName is null\n");
                        } else {
                            LOG.debug("createdServiceFunctionClassifier: relay node is {}\n", lastNodeName);
                        }
                        outPort = SfcOvsUtil.getVxlanOfPort(lastNodeName);
                        key = new StringBuffer();
                        key.append(scf.getName()).append(aclName).append(ruleName).append(".relay");
                        Ip ip = SfcOvsUtil.getSffVxlanDataLocator(sff);
                        reverseNsh.setVxlanIpDst(ip.getIp().getIpv4Address());
                        reverseNsh.setVxlanUdpPort(ip.getPort());
                        if (!SfcScfOfUtils.createClassifierRelayFlow(lastNodeName, key.toString(), reverseNsh)) {
                            LOG.error("createdServiceFunctionClassifier: fail to create relay flow\n");
                        }
                    }
                }
            }
        }
        return true;
    }

   /**
    * delete flows for service function classifier
    * The function returns true if successful.
    * The function returns false if unsuccessful.
    *
    * @param  scf service function classifier
    * @return          delete result
    */
    public boolean deletedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        if (scf == null) {
            LOG.error("deletedServiceFunctionClassifier: scf is null\n");
            return false;
        }
        LOG.debug("delete ServiceFunctionClassifier name: {} ACL: {} SFF: {}\n", scf.getName(), scf.getAcl(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAcl().getName(), scf.getAcl().getType());
        if (acl == null) {
            LOG.error("deletedServiceFunctionClassifier: acl is null\n");
            return false;
        }

        String aclName = acl.getAclName();
        if (aclName == null) {
            LOG.error("deletedServiceFunctionClassifier: aclName is null\n");
            return false;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.error("deletedServiceFunctionClassifier: accessListEntries is null\n");
            return false;
        }
        List<Ace> acesList = accessListEntries.getAce();
        if (acesList == null) {
            LOG.error("deletedServiceFunctionClassifier: acesList is null\n");
            return false;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("deletedServiceFunctionClassifier: sfflist is null\n");
            return false;
        }

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                if (ruleName == null) {
                    LOG.error("deletedServiceFunctionClassifier: ruleName is null\n");
                    continue;
                }

                // Action
                Actions actions = ace.getActions();
                if (actions == null) {
                    LOG.error("createdServiceFunctionClassifier: action is null\n");
                    continue;
                }

                Actions1 a1 = actions.getAugmentation(Actions1.class);
                if (a1 == null) {
                    LOG.error("createdServiceFunctionClassifier: action augment is null\n");
                    continue;
                }

                AclRenderedServicePath path = (AclRenderedServicePath) a1.getSfcAction();
                if (path == null) {
                    LOG.error("createdServiceFunctionClassifier: sfc action is null\n");
                    continue;
                }

                RspName rspName = new RspName(path.getRenderedServicePath());
                SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName);

                SffName sffName = new SffName(sclsff.getName());

                ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
                if (sff == null) {
                    LOG.error("deletedServiceFunctionClassifier: sff is null\n");
                    continue;
                }

                SffOvsBridgeAugmentation ovsSff = sff.getAugmentation(SffOvsBridgeAugmentation.class);
                if (ovsSff == null) {
                    LOG.debug("deletedServiceFunctionClassifier: sff is not ovs\n");
                    continue;
                }

                String nodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(sff);
                if (nodeName == null) {
                    LOG.error("deletedServiceFunctionClassifier: nodeName is null\n");
                    continue;
                }

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName).append(".out");
                SfcScfOfUtils.deleteClassifierFlow(nodeName, key.toString());

                RspName reverseRspName = null;
                if (path.getRenderedServicePath().endsWith("-Reverse")) {
                    reverseRspName = new RspName(path.getRenderedServicePath().replaceFirst("-Reverse", ""));
                } else {
                    reverseRspName = new RspName(path.getRenderedServicePath() + "-Reverse");
                }
                SfcNshHeader reverseNsh = SfcNshHeader.getSfcNshHeader(reverseRspName);

                if (reverseNsh == null) {
                    LOG.debug("deletedServiceFunctionClassifier: reverseNsh is null\n");
                } else {
                    key = new StringBuffer();
                    key.append(scf.getName()).append(aclName).append(ruleName).append(".in");
                    if (!SfcScfOfUtils.deleteClassifierFlow(nodeName, key.toString())) {
                        LOG.error("deletedServiceFunctionClassifier: fail to delete in flow\n");
                    }

                    SffName lastSffName = reverseNsh.getSffName();
                    if (lastSffName != null &&
                        !reverseNsh.getSffName().equals(sffName)) {
                        ServiceFunctionForwarder lastSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
                        String lastNodeName = SfcOvsUtil.getOpenFlowNodeIdForSff(lastSff);
                        if (lastNodeName == null) {
                            LOG.error("deletedServiceFunctionClassifier: lastNodeName is null\n");
                        } else {
                            LOG.debug("deletedServiceFunctionClassifier: relay node is {}\n", lastNodeName);
                        }
                        key = new StringBuffer();
                        key.append(scf.getName()).append(aclName).append(ruleName).append(".relay");
                        if (!SfcScfOfUtils.deleteClassifierFlow(lastNodeName, key.toString())) {
                            LOG.error("deletedServiceFunctionClassifier: fail to delete relay flow\n");
                        }
                    }
                }
            }
        }
        return true;
    }
}
