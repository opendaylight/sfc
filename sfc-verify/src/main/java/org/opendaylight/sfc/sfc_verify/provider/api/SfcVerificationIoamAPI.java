/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
//import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;

import org.opendaylight.sfc.sfc_verify.utils.SfcVerifyNetconfReaderWriterAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ProfileIndexRange;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ProfileNumRange;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ScvProfiles;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.ScvProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.scv.profiles.ScvProfile;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.scv.profiles.ScvProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.scv.profiles.scv.profile.Coefficients;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.scv.profiles.scv.profile.CoefficientsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.scv.profiles.scv.profile.ServiceChainIndices;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.scv.rev151221.scv.profiles.scv.profile.ServiceChainIndicesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Matches1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.service.function.classifier.SclServiceFunctionForwarder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.PolyAlg;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.RspSfcvAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.RspSfcvHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.AlgorithmParameters;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.PolyParams;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.PolyParameters;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.PolyParameter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Coeffs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Indices;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.hop.secret.algorithm.type.Poly;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.hop.secret.algorithm.type.poly.PolySecrets;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.hop.secret.algorithm.type.poly.poly.secrets.PolySecret;

//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.MatchesBuilder;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;


/**
 * This class is used to handle south-bound configuration generation for SFC verification.
 *
 * @author Xiao Liang, Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationIoamAPI
 * @since 2015-08-26
 */
public class SfcVerificationIoamAPI {
    private static final String ACE_IP = "AceIp";
    private static final String ACE_IPV6 = "AceIpv6";
    private static final long APP_DATA_VAL = 16;
    private static final int SB_MARK_VALUE = 7;
    private static final long SB_BITMASK_VALUE = 32;
    private final SfcVerifyNodeManager nodeManager;
    private final static Logger LOG = LoggerFactory.getLogger(SfcVerificationIoamAPI.class);

    public static final InstanceIdentifier<ScvProfiles> SCV_PROFILES_IID =
            InstanceIdentifier.create(ScvProfiles.class);

    public static final InstanceIdentifier<AccessLists> ACL_IID =
            InstanceIdentifier.create(AccessLists.class);

    private static class Config {
        NodeId nodeId;
        InstanceIdentifier iid;
        Config(NodeId nodeId, InstanceIdentifier iid) {
            this.nodeId = nodeId;
            this.iid = iid;
        }
    }

    public Map<String, List<Config>> pathConfig;

    public SfcVerificationIoamAPI(SfcVerifyNodeManager nodeManager) {
        pathConfig = new HashMap<>();
        this.nodeManager = nodeManager;
    }

    /*
    public SfcVerificationIoamAPI() {
        pathConfig = new HashMap<>();
    }*/

    /**
     * Returns an boolean for whether a specified capability exists in the
     * capability list sent by the netconf server node.
     *
     * @param nodeId     Netconf server node's id.
     * @param capability Capability to be checked.
     * @return boolean
     */
    private boolean checkCapability(NodeId nodeId, String capability) {
        NetconfNode netconfNode = SfcVerifyNetconfReaderWriterAPI.readNode(nodeId);
        if (netconfNode != null) {
            for (String cap : netconfNode.getAvailableCapabilities().getAvailableCapability()) {
                if (cap.endsWith(capability)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns an PolyParameters object representing the SFCV related augmentation from RSP.
     *
     * @param rsp Rendered Service Path from which augmentation needs to be got.
     * @return PolyParameters object.
     * @see service-function-chain-verification.yang
     */
    private PolyParameters getSfcvParameters(RenderedServicePath rsp) {
        PolyParameters params = null;
        RspSfcvAugmentation sfcvAugmentation = rsp.getAugmentation(RspSfcvAugmentation.class);
        if (sfcvAugmentation != null) {
            AlgorithmParameters algorithmParameters = sfcvAugmentation.getAlgorithmParameters();
            if (sfcvAugmentation.getSfcvAlgorithm().equals(PolyAlg.class) &&
                    algorithmParameters instanceof PolyParams) {
                params = ((PolyParams) algorithmParameters).getPolyParameters();
            }
        }
        return params;
    }

    private List<ServiceChainIndices> getServiceIndices(PolyParameter params) {
        ArrayList<ServiceChainIndices> indices = new ArrayList<>();
        for (Indices i : params.getIndices()) {
            indices.add(new ServiceChainIndicesBuilder().setIndex(i.getIndex()).build());
        }
        return indices;
    }

    private List<Coefficients> getCoefficients(PolyParameter params) {
        ArrayList<Coefficients> coeffs = new ArrayList<>();
        for (Coeffs coeff : params.getCoeffs()) {
            coeffs.add(new CoefficientsBuilder().setCoefficient(BigInteger.valueOf(coeff.getCoeff())).build());
        }
        return coeffs;
    }


    private ScvProfiles buildProfile(String name, String acl, String rspName, PolyParameters params,
                                     PolySecrets secrets, long startIndex, long validity, int posIndex) {
        long numProfiles = params.getNumPolyParameter();
        long baseNumProfiles = params.getBaseNumProfiles();
        List<ServiceChainIndices> indices;
        List<Coefficients> coeffs;
        long count = 1;
        Long maskval = Long.valueOf(SB_BITMASK_VALUE);

        List<PolyParameter> paramList = params.getPolyParameter();
        List<PolySecret> secretList = secrets.getPolySecret();

        ScvProfilesBuilder sbuilder = new ScvProfilesBuilder();

        ArrayList<ScvProfile> scvProfileList = new ArrayList<>();
        for (long j = startIndex; ;) {
            ScvProfileBuilder builder = new ScvProfileBuilder();

            /* paramList from RSP typically seems to be out of order in its contents. Correct
             * the order and use it.
             */
            PolyParameter paramObj = null;
            for (int k = 0; k < paramList.size(); k++) {
                if (paramList.get(k).getPindex() == j) {
                    LOG.debug("buildProfile: param: actual index:{}, should have been:{}", k, j);
                    paramObj = paramList.get(k);
                }
            }

            PolySecret secretObj = null;
            for (int m = 0; m < secretList.size(); m++) {
                if (secretList.get(m).getPindex() == j) {
                    LOG.debug("buildProfile: secrets: actual index:{}, should have been:{}", m, j);
                    secretObj= secretList.get(m);
                }
            }

            indices = getServiceIndices(paramObj);
            coeffs  = getCoefficients(paramObj);

            builder.setServiceProfileIndex(new ProfileIndexRange(Long.valueOf(j)))
                   .setServiceProfileName((name +"-Idx-"+j))
                   .setAclName(acl)
                   .setPrimeNumber(BigInteger.valueOf(paramObj.getPrime()))
                   .setSecretShare(BigInteger.valueOf(secretObj.getSecretShare()))
                   .setServiceCount(indices.size())
                   .setCoefficients(coeffs)
                   .setServiceChainIndices(indices)
                   .setValidity(BigInteger.valueOf(validity))
                   .setMark(Integer.valueOf(SB_MARK_VALUE))
                   .setBitmask(new BigInteger(maskval.toString()))
                   .setMyServiceIndex(secretList.get(posIndex).getIndex());
            if (secretObj.getSecret() != null) {
                builder.setValidator(true).setValidatorKey(BigInteger.valueOf(secretObj.getSecret()));
            } else {
                builder.setValidator(false);
            }

            scvProfileList.add(builder.build());

            /* ring buffer */
            j++;
            j = (j % baseNumProfiles);

            /* count number of times and quit */
            if (count == numProfiles) break;
            count++;
        }

        sbuilder.setRenderedServicePathName(new RspName(rspName))
                .setScvProfileStartIndex(new ProfileIndexRange(Long.valueOf(startIndex)))
                .setScvProfileNumProfiles(new ProfileNumRange(Long.valueOf(numProfiles)))
                .setScvProfile(scvProfileList);
        return sbuilder.build();
    }

    private void configSF(List<Config> configList, final NodeId nodeId, ScvProfiles profile) {
        if (!checkCapability(nodeId, "ioam-scv")) {
            LOG.warn("Node {} does not have ioam-scv capability", nodeId.getValue());
            return;
        }
        LOG.info("configSF: Sending SFCV config:{} to node:{}",profile, nodeId.getValue());
        InstanceIdentifier iid = SCV_PROFILES_IID;
        if (SfcVerifyNetconfReaderWriterAPI.put(nodeId, LogicalDatastoreType.CONFIGURATION, iid, profile)) {
            LOG.info("Successfully configured SF node {}", nodeId.getValue());
            configList.add(new Config(nodeId, iid));
        } else {
            LOG.error("Error configuring SF node {} through NETCONF", nodeId.getValue());
        }
    }

    private void configSFF(List<Config> configList, final NodeId nodeId, Acl acl) {
        if (!checkCapability(nodeId, "ietf-acl")) {
            LOG.warn("Node {} does not have ietf-acl capability", nodeId.getValue());
            return;
        }
        InstanceIdentifier iid = ACL_IID.child(Acl.class, acl.getKey());
        if (SfcVerifyNetconfReaderWriterAPI.put(nodeId, LogicalDatastoreType.CONFIGURATION, iid, acl)) {
            LOG.info("Successfully configured SFF node {}", nodeId.getValue());
            configList.add(new Config(nodeId, iid));
        } else {
            LOG.error("Error configuring SFF node {} through NETCONF", nodeId.getValue());
        }
    }

    private Acl removeAclAugmentation(Acl acl) {

        if (acl == null) {
            return null;
        }

        List<Ace> aceList = acl.getAccessListEntries().getAce();
        AccessListEntriesBuilder entriesBuilder = new AccessListEntriesBuilder();

        for (Ace entry : aceList) {
            Actions actions = entry.getActions();
            Matches matches = entry.getMatches();
            boolean changed = false;
            if (actions != null && actions.getAugmentation(Actions1.class) != null) {
                actions = new ActionsBuilder(actions).removeAugmentation(Actions1.class).build();
                changed = true;
            }
            if (matches != null && matches.getAugmentation(Matches1.class) != null) {
                matches = new MatchesBuilder(matches).removeAugmentation(Matches1.class).build();
                changed = true;
            }

            List<Ace> aceNewList = new ArrayList<>();
            if (changed) {
                AceBuilder aceb = new AceBuilder();
                aceb.setRuleName(entry.getRuleName())
                    .setActions(actions)
                    .setMatches(matches);

                aceNewList.add(aceb.build());

                entriesBuilder.setAce(aceNewList);

            } else {
                aceNewList.add(entry);
                entriesBuilder.setAce(aceNewList);
            }
        }
        return new AclBuilder(acl).setAccessListEntries(entriesBuilder.build()).build();
    }


    /*
     * This function processes RSP updates to send out related configuration for
     * SFCV creation, renewal or refresh configuration options.
     */
    public void processRspUpdate(RenderedServicePath rsp) {
        Preconditions.checkNotNull(rsp);
        PolyParameters params = getSfcvParameters(rsp);
        String aclName;
        Acl acl;
        String rspName = rsp.getName().getValue();
        List<Config> configList = new ArrayList<>();
        Set<String> sclSffs = new HashSet<>();
        String srcNodeId = null;
        int i = 0;
        int posIndex=0;
        SffName sffName;
        long startIndex;
        long validity;

        if (params == null) {
            LOG.debug("SFC verification params not present for: {}", rsp.getName());
            //To handle the case where the RSP is updated to remove the SFCV configuration
            //but leave the RSP itself without any other changes, delete the SFCV
            //configuration if present.
            deleteRsp(rsp);
            return;
        }

        startIndex = params.getStartIndex();
        validity   = params.getProfilesValidator();

        srcNodeId = params.getRefreshNode();

        ServiceFunctionPath sfp = SfcProviderServicePathAPI.
                                  readServiceFunctionPath(rsp.getParentServiceFunctionPath());
        ServiceFunctionClassifier classifier = SfcProviderServiceClassifierAPI.
                                  readServiceClassifier(sfp.getClassifier());
        for (SclServiceFunctionForwarder sclSff : classifier.getSclServiceFunctionForwarder()) {
            sclSffs.add(sclSff.getName());
        }

        aclName = classifier.getAccessList();
        acl = removeAclAugmentation(SfcProviderAclAPI.readAccessList(aclName));
        if (acl == null) {
            LOG.info("ACL not available for {}", rsp.getName());
        }

        List<RenderedServicePathHop> hopList = rsp.getRenderedServicePathHop();
        boolean powEnable = false;

        if (hopList != null) {
            for (RenderedServicePathHop h : hopList) {
                NodeId sffNode = null;
                sffName = h.getServiceFunctionForwarder();
                sffNode = getSffNodeId(sffName);
                if (sffNode == null) {
                    LOG.error("sffNode is null for sffName: {}", sffName);
                    return;
                }

                RspSfcvHopAugmentation sfcvHopAugmentation = h.getAugmentation(RspSfcvHopAugmentation.class);

                if (sfcvHopAugmentation != null &&
                        sfcvHopAugmentation.getAlgorithmType().getImplementedInterface().equals(Poly.class)) {
                    PolySecrets secrets = ((Poly)sfcvHopAugmentation.getAlgorithmType()).getPolySecrets();
                    if (srcNodeId != null) {
                        if (!srcNodeId.equals("")) {
                            if (!((sffNode.toString()).equals(srcNodeId))) {
                                LOG.debug("processRspUpdate:Targetting nodeid:{}, hence skipping: {}",
                                          srcNodeId, sffNode);
                                continue;
                            }
                        }
                    }
                    ScvProfiles profile =
                        buildProfile(rspName + '-' + h.getServiceIndex(), aclName, rspName, params,
                                     secrets, startIndex, validity, posIndex);
                    configSF(configList, sffNode, profile);
                    powEnable = true;
                    posIndex++;
                }

                if (sclSffs.contains(h.getServiceFunctionForwarder().getValue())) {
                    if (acl != null) {
                        configSFF(configList, sffNode, acl);
                    }
                }
            }
        }
        pathConfig.put(rspName, configList);

    }

    /*
     * This function processes RSP deletes to send out related configuration for
     * SFCV deletion configurations to the nodes.
     */
    public void deleteRsp(RenderedServicePath rsp) {
        List<Config> configList = pathConfig.get(rsp.getName().getValue());
        if (configList != null) {
            for (Config cfg : configList) {
                SfcVerifyNetconfReaderWriterAPI.delete(cfg.nodeId, LogicalDatastoreType.CONFIGURATION, cfg.iid);
            }
            pathConfig.remove(rsp.getName().getValue());
        }
    }


    private NodeId getSffNodeId(SffName sffName) {
        // Read SFF from Controller CONF
        org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder sfcForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        if (sfcForwarder == null) {
            LOG.error("SFF name {} not found in data store", sffName.getValue());
            return null;
        }
        IpAddress sffMgmtIp = sfcForwarder.getIpMgmtAddress();
        if (sffMgmtIp == null) {
            LOG.error("Unable to obtain management IP for SFF {}", sffName.getValue());
            return null;
        }

        return nodeManager.getNodeIdFromIpAddress(new IpAddress(new Ipv4Address(sffMgmtIp.getIpv4Address()
                .getValue())));
    }
}
