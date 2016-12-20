/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.provider;

import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;

import org.opendaylight.sfc.pot.netconf.renderer.provider.api.SfcPotPolyAPI;
import org.opendaylight.sfc.pot.netconf.renderer.utils.SfcPotNetconfReaderWriterAPI;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.BitMaskOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.RspIoamPotAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.TimeResolution;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.params.rev161205.PolyParameters;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.params.rev161205.poly.parameters.PolyParameter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.params.rev161205.poly.parameters.poly.parameter.Coeffs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.params.rev161205.poly.parameters.poly.parameter.Lpcs;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.hop.params.rev161205.PolySecrets;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.hop.params.rev161205.poly.secrets.PolySecret;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.PotProfiles;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.PotProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileListBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSet;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.ProfileIndexRange;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to send south-bound configuration SFC PoT via Netconf.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-12-01
 */
public class SfcPotNetconfIoam {
    private final SfcPotNetconfNodeManager nodeManager;

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfIoam.class);

    private static final long MIN_CFG_REFRESH_INTERVAL_MS  =  500;
    private static final long DEF_CFG_REFRESH_INTERVAL_MS  =  5000;
    private static final long CFG_REFRESH_INTERVAL_FUZZ_MS =  250;
    private static final long DEF_NUM_POT_PROFILES = 2;
    private static final long MIN_SFC_SIZE = 3;

    public static final InstanceIdentifier<PotProfiles> POT_PROFILES_IID =
            InstanceIdentifier.create(PotProfiles.class);

    private static class Config {
        NodeId nodeId;
        InstanceIdentifier iid;
        DataBroker broker;
        Config(NodeId nodeId, InstanceIdentifier iid, DataBroker broker) {
            this.nodeId = nodeId;
            this.iid = iid;
            this.broker = broker;
        }
    }

    /* Stores configuration map to allow deletes later */
    public Map<String, HashSet<Config>> pathConfig;

    public SfcPotNetconfIoam(SfcPotNetconfNodeManager nodeManager) {
        pathConfig = new HashMap<>();
        this.nodeManager = nodeManager;
    }

    /* Utility functions */
    private List<BigInteger> getCoefficients(PolyParameter params) {
        ArrayList<BigInteger> coeffs = new ArrayList<>();
        for (Coeffs coeff : params.getCoeffs()) {
            coeffs.add(BigInteger.valueOf(coeff.getCoeff()));
        }
        return coeffs;
    }

    private List<Long> getLpcs(PolyParameter params) {
        ArrayList<Long> lpcs = new ArrayList<>();
        for (Lpcs lpc: params.getLpcs()) {
            lpcs.add(lpc.getLpc());
        }
        return lpcs;
    }

    private Short getBitMaskValue (BitMaskOptions bitMask) {
        short val;
        switch (bitMask) {
            case Bits16:
                val = 16;
                break;
            case Bits32:
                val = 32;
                break;
            case Bits64:
                val = 64;
                break;
            default:
                val = 32;
        }

        return val;
    }

    /**
     * Utility function that handles common processing to send configuration to nodes.
     *
     * @return current active index.
     */
    private int sendIoamPotConfig (RenderedServicePath rsp, SfcPotPolyAPI potApi,
            HashSet<Config> configHash,  int newActiveIndex) {
        int posIndex = 0;
        String rspName = rsp.getName().getValue();
        SffName sffName;

        PolyParameters ioamPotParams = potApi.getIoamPotParameters(0);
        if (ioamPotParams == null) {
            LOG.warn("iOAM:PoT:SB:profile parameters not present in RSP:{}", rsp.getName());
            return -1;
        }

        List<RenderedServicePathHop> hopList = rsp.getRenderedServicePathHop();
        for (RenderedServicePathHop h : hopList) {
            NodeId sffNode = null;
            sffName = h.getServiceFunctionForwarder();
            sffNode = getSffNodeId(sffName);
            if (sffNode == null) {
                LOG.warn("iOAM:PoT:SB:sffNode is null for sffName: {}", sffName);
                return -1;
            }

            PolySecrets ioamPotHopParams = potApi.getIoamPotHopParameters(posIndex);
            if (ioamPotHopParams == null) {
                LOG.warn("iOAM:PoT:SB:Hop parameters not available for RSP:{}",
                    rsp.getName());
                return -1;
            }

            /* Initially, all profiles are downloaded with invalid activeIndex */
            PotProfiles profile =
                buildProfile(rspName + '-' + h.getServiceIndex(), ioamPotParams,
                             ioamPotHopParams, posIndex, -1);
            if (!configSF(configHash, sffNode, profile)) {
                /* Error already logged */
                return -1;
            }
            posIndex++;
        }

        /* When the initial download is successful, the first node of the hop is
         * notified of what is the activeIndex.  This is as per the protocol required
         * at the SB nodes.
         */
        posIndex = 0;

        RenderedServicePathHop hop = hopList.iterator().next();
        NodeId sffNode = null;
        sffName = hop.getServiceFunctionForwarder();
        sffNode = getSffNodeId(sffName);
        if (sffNode == null) {
            LOG.warn("iOAM:PoT:SB:sffNode is null for sffName: {}", sffName);
            return -1;
        }

        PolySecrets ioamPotHopParams = potApi.getIoamPotHopParameters(posIndex);
        if (ioamPotHopParams == null) {
            LOG.warn("iOAM:PoT:SB: Hop parameters cannot be got for RSP:{}", rsp.getName());
            return -1;
        }

        /* profiles are updated at the first node */
        PotProfiles profile =
            buildProfile(rspName + '-' + hop.getServiceIndex(), ioamPotParams,
                ioamPotHopParams, posIndex, newActiveIndex);
        if (!configSF(configHash, sffNode, profile)) {
            /* Error already logged */
            return -1;
        }

        return newActiveIndex;
    }

    /**
     * Utility function that handles common processing for config creation and re-generation.
     *
     * @return Current Active Index.
     */
    private int processRspIoamPot (RenderedServicePath rsp,
            int sfcSize, final Class<? extends TimeResolution> refreshPeriodTimeUnits,
            Long refreshPeriodValue, BitMaskOptions ioamPotProfileBitMask,
            Long ioamPotNumProfiles, int currActiveIndex, boolean flagRenew) {

        Long adjRefreshPeriodValue;
        int newActiveIndex = currActiveIndex;
        String rspName = rsp.getName().getValue();
        HashSet<Config> configHash = new HashSet<Config>();

        SfcPotPolyAPI potApi = SfcPotPolyAPI.getInstance();
        if (potApi == null) {
            LOG.warn("iOAM:PoT:SB: Error in initialization.");
            return -1;
        }

        if (!flagRenew) {
            if (!potApi.init(rsp.getName().getValue(), sfcSize,
                    refreshPeriodTimeUnits, refreshPeriodValue, ioamPotProfileBitMask,
                    ioamPotNumProfiles)) {
                LOG.warn("iOAM:PoT:SB: Profile not initialized for RSP:{}", rsp.getName());
                return -1;
            }
        } else {
            newActiveIndex = potApi.initRenew(rsp.getName().getValue(), sfcSize, currActiveIndex);
            if (newActiveIndex == -1) {
                LOG.warn("iOAM:PoT:SB: Profile could not be renewed for RSP:{}", rsp.getName());
                return -1;
            }
        }

        int ret = sendIoamPotConfig (rsp, potApi, configHash, newActiveIndex);
        if (ret == -1) {
            LOG.warn("iOAM:PoT:SB:profile could not be sent for RSP:{}", rsp.getName());
            return -1;
        }

        /* Initial config send: Set up timer for periodic config regeneration */
        if (!flagRenew) {
            /* Set up timer for periodic config regeneration */
            SfcPotTimerTask potTimerTask = new SfcPotTimerTask(rsp.getName());
            SfcPotTimerWheel potTimerWheel = SfcPotTimerWheel.getInstance();
            SfcPotTimerData potTimerData = SfcPotTimerData.getInstance();

            /* NOTE: This is done to start a timer for a period a bit before the
             * actual refresh timer expiry so that config refresh can happen
             * before the configurations expire at the nodes.
             */
            /* NOTE:TODO: support other time units. Currently assume ms */
            adjRefreshPeriodValue = (refreshPeriodValue - CFG_REFRESH_INTERVAL_FUZZ_MS);

            Timeout potTimeout = potTimerWheel.setTimerContext(potTimerTask,
                                                               adjRefreshPeriodValue,
                                                               TimeUnit.MILLISECONDS);
            /* Start off with active cfg index 0 */
            potTimerData.addRspData(rsp.getName(), adjRefreshPeriodValue, refreshPeriodTimeUnits,
                                    0, sfcSize, potTimerTask, potTimeout);

            LOG.debug("iOAM:PoT:SB:Started timer for RSP:{}, sfc:{}", rsp.getName(), sfcSize);

            pathConfig.put(rspName, configHash);

            return 0;
        }

        return newActiveIndex;
    }

    /**
     * Returns an PotProfiles object representing the PoT related configuration.
     *
     * @return PotProfiles object.
     */
    private PotProfiles buildProfile(String profileName, PolyParameters params,
            PolySecrets secrets, int posIndex, int activeIndex) {
        List<BigInteger> coeffs;
        List<Long> lpcs;
        List<PolyParameter> paramList = params.getPolyParameter();
        List<PolySecret> secretList = secrets.getPolySecret();
        BitMaskOptions ioamPotProfileBitMask = params.getProfileBitMaskValue();
        Short bitMaskValue = getBitMaskValue(ioamPotProfileBitMask);

        long numProfiles = paramList.size();

        PotProfilesBuilder pbuilder = new PotProfilesBuilder();

        ArrayList<PotProfileList> potProfileList = new ArrayList<>();

        for (int j = 0; j < numProfiles; j++) {
            PotProfileListBuilder builder = new PotProfileListBuilder();

            PolyParameter paramObj = paramList.get(j);
            PolySecret    secretObj = secretList.get(j);

            coeffs  = getCoefficients(paramObj);
            lpcs    = getLpcs(paramObj);

            builder.setIndex(new ProfileIndexRange(Integer.valueOf(j)))
                   .setPrimeNumber(BigInteger.valueOf(paramObj.getPrime()))
                   .setLpc(BigInteger.valueOf(lpcs.get(posIndex)))
                   .setSecretShare(BigInteger.valueOf(secretObj.getSecretShare()))
                   .setPublicPolynomial(coeffs.get(posIndex))
                   .setNumberOfBits(bitMaskValue);

            if (secretObj.getSecret() != null) {
               builder.setValidator(true).setValidatorKey(BigInteger.valueOf(secretObj.
                   getSecret()));
            } else {
               builder.setValidator(false);
            }

            potProfileList.add(builder.build());
        }

        PotProfileSetBuilder sbuilder = new PotProfileSetBuilder();

        sbuilder.setPotProfileList(potProfileList)
                .setName(profileName)
                .setActiveProfileIndex(new ProfileIndexRange(Integer.valueOf(activeIndex)))
                .setPathIdentifier(null);

        ArrayList<PotProfileSet> potProfileSet = new ArrayList<>();
        potProfileSet.add(sbuilder.build());
        pbuilder.setPotProfileSet(potProfileSet);

        return pbuilder.build();
    }

    /**
     * Sends out configuration to the SB node via Netconf
     *
     * @return Success or Failure.
     */
    private boolean configSF(HashSet<Config> configHash, final NodeId nodeId, PotProfiles profile) {
        InstanceIdentifier<PotProfiles> iid = POT_PROFILES_IID;

        DataBroker broker = nodeManager.getMountPointFromNodeId(nodeId);
        if (broker == null) {
            LOG.warn("iOAM:SB:PoT:Error configuring SF node. Broker invalid.");
            return false;
        }

        if (SfcPotNetconfReaderWriterAPI.put(broker, LogicalDatastoreType.CONFIGURATION,
               iid, profile)) {
            LOG.info("iOAM:SB:PoT:Successfully configured SF node {}", nodeId.getValue());
            /* Duplicates not added, as configSF could be called more than once for first node */
            configHash.add(new Config(nodeId, iid, broker));
        } else {
            LOG.warn("iOAM:SB:PoT:Error configuring SF node {} via NETCONF", nodeId.getValue());
            return false;
        }

        return true;
    }

    /* This function returns NodeId given the IP address of the node */
    private NodeId getSffNodeId(SffName sffName) {
        if (sffName == null) {
            LOG.warn("iOAM:PoT:SB:SFF name invalid");
            return null;
        }

        ServiceFunctionForwarder sfcForwarder = SfcProviderServiceForwarderAPI.
                readServiceFunctionForwarder(sffName);

        if (sfcForwarder == null) {
            LOG.warn("iOAM:PoT:SB:SFF name {} not found in data store", sffName.getValue());
            return null;
        }

        IpAddress sffMgmtIp = sfcForwarder.getIpMgmtAddress();
        if (sffMgmtIp == null) {
            LOG.warn("iOAM:PoT:SB:Unable to obtain management IP for SFF {}",
                    sffName.getValue());
            return null;
        }

        return nodeManager.getNodeIdFromIpAddress(new IpAddress(new Ipv4Address(sffMgmtIp.
                getIpv4Address().getValue())));
    }

    /*
     * This function processes RSP updates to send out related configuration for
     * PoT creation, renewal or refresh configuration options.
     */
    public void processRspUpdate(RenderedServicePath rsp) {
        final Class<? extends TimeResolution> refreshPeriodTimeUnits;
        SfcPotTimerData potTimerData = SfcPotTimerData.getInstance();

        if (rsp == null) {
            LOG.warn("iOAM:PoT:SB:RSP is invalid.");
            return;
        }

        String rspName = rsp.getName().getValue();

        SfpName sfpName = rsp.getParentServiceFunctionPath();
        if (sfpName == null) {
            LOG.warn("iOAM:PoT:SB:ServiceFunctionPath is invalid.");
            return;
        }

        ServiceFunctionPath sfp = SfcProviderServicePathAPI.
                                  readServiceFunctionPath(sfpName);

        SfcName serviceFunctionChainNameObj = sfp.getServiceChainName();
        ServiceFunctionChain serviceFunctionChain = serviceFunctionChainNameObj != null ?
                SfcProviderServiceChainAPI.readServiceFunctionChain(serviceFunctionChainNameObj)
                : null;
        if (serviceFunctionChain == null) {
            LOG.warn("iOAM:PoT:SB:SFC name for Path {} is null.", sfp.getName());
            return;
        }

        /* Count the number of service functions in the SFC. */
        int sfcSize = serviceFunctionChain.getSfcServiceFunction().size();
        if (sfcSize < MIN_SFC_SIZE) {
            LOG.warn("iOAM:PoT:SB:Service Function Chain size:{} is unsupported.", sfcSize);
            return;
        }

        RspIoamPotAugmentation potAugmentation = null;
        potAugmentation = rsp.getAugmentation(RspIoamPotAugmentation.class);
        if (potAugmentation == null) {
            LOG.warn("iOAM:PoT:SB:RSP {}:iOAM not supported.", rsp.getName());
            return;
        }

        if (!potAugmentation.isIoamPotEnable()) {
            LOG.info("iOAM:PoT:SB:RSP {}:iOAM Proof of Transit disabled.", rsp.getName());

            /* Disable timers to handle case of PoT disable */
            SfcPotTimerWheel timer = SfcPotTimerWheel.getInstance();
            timer.clearTimerContext(potTimerData.getRspDataTimeout(rsp.getName()));
            potTimerData.delRspData(rsp.getName());

            /* NOTE:TODO: send down disabled config. For now let config expire */
            return;
        }

        /* NOTE: TODO: iOAM PoT parameter updates not yet supported */
        if (potTimerData.isRspDataPresent(rsp.getName())) {
            LOG.warn("iOAM:PoT:SB:RSP {}:PoT parameters updates not supported.", rsp.getName());
            return;
        }

        refreshPeriodTimeUnits = potAugmentation.getRefreshPeriodTimeUnits();
        Long refreshPeriodValue = potAugmentation.getRefreshPeriodValue();
        BitMaskOptions ioamPotProfileBitMask = potAugmentation.getIoamPotBitMask();
        Long ioamPotNumProfiles = potAugmentation.getIoamPotNumProfiles();

        LOG.debug("iOAM:PoT:SB:RSP {}:Got iOAM Proof of Transit params:{}",
                rsp.getName(), potAugmentation);

        if (refreshPeriodValue < MIN_CFG_REFRESH_INTERVAL_MS) {
            LOG.warn("iOAM:PoT:SB:RSP {}: Got unsupported refresh period value:{}.using default.",
                rspName, refreshPeriodValue);
            refreshPeriodValue = DEF_CFG_REFRESH_INTERVAL_MS;
        }

        if (ioamPotNumProfiles < DEF_NUM_POT_PROFILES) {
            LOG.warn("iOAM:PoT:SB:RSP {}: Got unsupported profile num:{}...using default.",
                     rspName, ioamPotNumProfiles);
            ioamPotNumProfiles = DEF_NUM_POT_PROFILES;
        }

        /* Call the utility function to do the work */
        int ret = processRspIoamPot (rsp, sfcSize, refreshPeriodTimeUnits,
                      refreshPeriodValue, ioamPotProfileBitMask, ioamPotNumProfiles,
                      0, false);
        if (ret < 0) {
            LOG.warn("iOAM:PoT:SB:RSP:{} processing error in proof of transit.", rsp.getName());
        } else {
            LOG.debug("iOAM:PoT:SB:RSP:{} processing done for proof of transit.", rsp.getName());
        }
    }

    /*
     * This function processes RSP refresh requests to send out related configuration for
     * PoT renewal or refresh configuration options.
     */
    public int refreshSfcPot(RspName rspName, int currActiveIndex, int sfcSize,
            Long refreshPeriodValue) {
        RenderedServicePath rsp = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);

        if (rsp == null) {
            LOG.warn("iOAM:PoT:SB:RSP for SFC: {} is invalid.", rspName);
            return -1;
        }

        if (sfcSize == 0) {
            LOG.warn("iOAM:PoT:SB:Service Function Chain is empty.");
            return -1;
        }

        return (processRspIoamPot (rsp, sfcSize, null, refreshPeriodValue, null, null,
                currActiveIndex, true));
    }

    /*
     * This function processes RSP deletes to send out related configuration for
     * PoT deletion configurations to the nodes.
     */
    public void deleteRsp(RenderedServicePath rsp) {
        HashSet<Config> configHash = pathConfig.get(rsp.getName().getValue());
        SfcPotTimerData potTimerData = SfcPotTimerData.getInstance();
        DataBroker broker;

        /* Disable timers */
        SfcPotTimerWheel timer = SfcPotTimerWheel.getInstance();
        timer.clearTimerContext(potTimerData.getRspDataTimeout(rsp.getName()));
        potTimerData.delRspData(rsp.getName());

        if (configHash != null) {
            for (Config cfg : configHash) {
                LOG.debug("iOAM:PoT:SB:Cleaning up for node:{}", cfg.nodeId);
                SfcPotNetconfReaderWriterAPI.delete(cfg.broker,
                        LogicalDatastoreType.CONFIGURATION, cfg.iid);
            }
            pathConfig.remove(rsp.getName().getValue());
        }
    }
}
