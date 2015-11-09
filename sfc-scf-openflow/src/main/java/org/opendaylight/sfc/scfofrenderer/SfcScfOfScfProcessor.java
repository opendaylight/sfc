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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.attachment.point.attachment.point.type.Interface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcScfOfScfProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcScfOfScfProcessor.class);

    public SfcScfOfScfProcessor() {}

    public void createdServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        if (scf == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: scf is null");
            return;
        }

        LOG.debug("\ncreate ServiceFunctionClassifier name: {} ACL: {} SFF: {}", scf.getName(), scf.getAccessList(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAccessList());
        if (acl == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: acl is null");
            return;
        }

        String aclName = acl.getAclName();
        if (aclName == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: aclName is null");
            return;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: accessListEntries is null");
            return;
        }

        List<Ace> acesList = accessListEntries.getAce();
        if (acesList == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: acesList is null");
            return;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("\ncreatedServiceFunctionClassifier: sfflist is null");
            return;
        }

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            SffName sffName = new SffName(sclsff.getName());

            Long inPort = null;

            ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (sff == null) {
                LOG.error("\ncreatedServiceFunctionClassifier: sff is null");
                continue;
            }

            String nodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(sff);
            if (nodeName == null) {
                LOG.error("\ncreatedServiceFunctionClassifier: nodeName is null");
                continue;
            }

            Long outPort = SfcOvsPortUtils.getVxlanOfPort(nodeName);
            SfcScfOfUtils.initClassifierTable(nodeName);

            if (sclsff.getAttachmentPointType() instanceof Interface) {
                Interface itf = (Interface) sclsff.getAttachmentPointType();
                if (itf == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: attachment point is null");
                    continue;
                }

                String itfName = itf.getInterface();
                if (itf == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: interface is null");
                    continue;
                }
                inPort = SfcOvsPortUtils.getOfPortByName(nodeName, itfName);
                if (inPort == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: port is null");
                    continue;
                }
            }

            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                if (ruleName == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: ruleName is null");
                    continue;
                }

                StringBuffer sb = new StringBuffer();
                sb.append(nodeName).append(":");
                sb.append(String.valueOf(inPort));
                NodeConnectorId port = new NodeConnectorId(sb.toString());

                // Match
                Match match = new SfcMatch()
                                  .setPortMatch(port)
                                  .setAclMatch(ace.getMatches())
                                  .build();

                // Action
                Actions actions = ace.getActions();
                if (ruleName == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: action is null");
                    continue;
                }

                Actions1 a1 = actions.getAugmentation(Actions1.class);
                if (a1 == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: action augment is null");
                    continue;
                }

                AclRenderedServicePath path = (AclRenderedServicePath) a1.getSfcAction();
                if (path == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: sfc action is null");
                    continue;
                }

                RspName rspName = new RspName(path.getRenderedServicePath());
                SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName);


                if (nsh == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: nsh is null");
                    continue;
                }

                StringBuffer key = new StringBuffer();
                key.append(scf.getName()).append(aclName).append(ruleName).append(".out");
                if (!SfcScfOfUtils.createClassifierOutFlow(nodeName, key.toString(), match, nsh, outPort)) {
                    LOG.error("\ncreatedServiceFunctionClassifier: out flow is null");
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
                    LOG.debug("\ncreatedServiceFunctionClassifier: reverseNsh is null");
                } else {
                    key = new StringBuffer();
                    key.append(scf.getName()).append(aclName).append(ruleName).append(".in");
                    if (!SfcScfOfUtils.createClassifierInFlow(nodeName, key.toString(), reverseNsh, inPort)) {
                        LOG.error("\ncreatedServiceFunctionClassifier: fail to create in flow");
                    }

                    SffName lastSffName = reverseNsh.getSffName();
                    if (lastSffName != null &&
                        !reverseNsh.getSffName().equals(sffName)) {
                        ServiceFunctionForwarder lastSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
                        String lastNodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(lastSff);
                        if (lastNodeName == null) {
                            LOG.error("\ncreatedServiceFunctionClassifier: lastNodeName is null");
                        }
                        outPort = SfcOvsPortUtils.getVxlanOfPort(lastNodeName);
                        key = new StringBuffer();
                        key.append(scf.getName()).append(aclName).append(ruleName).append(".relay");
                        Ip ip = SfcOvsPortUtils.getSffIpDataLocator(sff, VxlanGpe.class);
                        reverseNsh.setVxlanIpDst(ip.getIp().getIpv4Address());
                        reverseNsh.setVxlanUdpPort(ip.getPort());
                        if (!SfcScfOfUtils.createClassifierRelayFlow(lastNodeName, key.toString(), reverseNsh)) {
                            LOG.error("\ncreatedServiceFunctionClassifier: fail to create relay flow");
                        }
                    }
                }
            }
        }
    }

    public void deletedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        if (scf == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: scf is null");
            return;
        }
        LOG.debug("\ndelete ServiceFunctionClassifier name: {} ACL: {} SFF: {}", scf.getName(), scf.getAccessList(),
                scf.getSclServiceFunctionForwarder());

        Acl acl = SfcProviderAclAPI.readAccessList(scf.getAccessList());
        if (acl == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: acl is null");
            return;
        }

        String aclName = acl.getAclName();
        if (aclName == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: aclName is null");
            return;
        }

        AccessListEntries accessListEntries = acl.getAccessListEntries();
        if (accessListEntries == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: accessListEntries is null");
            return;
        }
        List<Ace> acesList = accessListEntries.getAce();
        if (acesList == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: acesList is null");
            return;
        }

        List<SclServiceFunctionForwarder> sfflist = scf.getSclServiceFunctionForwarder();
        if (sfflist == null) {
            LOG.error("\ndeletedServiceFunctionClassifier: sfflist is null");
            return;
        }

        for (SclServiceFunctionForwarder sclsff : sfflist) {
            for (Ace ace : acesList) {

                String ruleName = ace.getRuleName();
                if (ruleName == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: ruleName is null");
                    continue;
                }

                         // Action
                Actions actions = ace.getActions();
                if (ruleName == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: action is null");
                    continue;
                }

                Actions1 a1 = actions.getAugmentation(Actions1.class);
                if (a1 == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: action augment is null");
                    continue;
                }

                AclRenderedServicePath path = (AclRenderedServicePath) a1.getSfcAction();
                if (path == null) {
                    LOG.error("\ncreatedServiceFunctionClassifier: sfc action is null");
                    continue;
                }

                RspName rspName = new RspName(path.getRenderedServicePath());
                SfcNshHeader nsh = SfcNshHeader.getSfcNshHeader(rspName);

                SffName sffName = new SffName(sclsff.getName());

                ServiceFunctionForwarder sff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
                if (sff == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: sff is null");
                    continue;
                }
                sff = SfcOvsUtil.augmentSffWithOpenFlowNodeId(sff);
                if (sff == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: sff augment is null");
                    continue;
                }
                String nodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(sff);
                if (nodeName == null) {
                    LOG.error("\ndeletedServiceFunctionClassifier: nodeName is null");
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
                    LOG.debug("\ndeletedServiceFunctionClassifier: reverseNsh is null");
                } else {
                    key = new StringBuffer();
                    key.append(scf.getName()).append(aclName).append(ruleName).append(".in");
                    if (!SfcScfOfUtils.deleteClassifierFlow(nodeName, key.toString())) {
                        LOG.error("\ndeletedServiceFunctionClassifier: fail to delete in flow");
                    }

                    SffName lastSffName = reverseNsh.getSffName();
                    if (lastSffName != null &&
                        !reverseNsh.getSffName().equals(sffName)) {
                        ServiceFunctionForwarder lastSff = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(lastSffName);
                        String lastNodeName = SfcOvsPortUtils.getSffOpenFlowNodeName(lastSff);
                        if (lastNodeName == null) {
                            LOG.error("\ndeletedServiceFunctionClassifier: lastNodeName is null");
                        }
                        key = new StringBuffer();
                        key.append(scf.getName()).append(aclName).append(ruleName).append(".relay");
                        if (!SfcScfOfUtils.deleteClassifierFlow(lastNodeName, key.toString())) {
                            LOG.error("\ndeletedServiceFunctionClassifier: fail to delete relay flow");
                        }
                    }
                }
            }
        }
    }

    public void updatedServiceFunctionClassifier(ServiceFunctionClassifier scf) {
        // TBD
    }
}
