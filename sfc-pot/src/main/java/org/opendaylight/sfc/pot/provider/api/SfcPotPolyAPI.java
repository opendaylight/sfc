/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.BitMaskOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.PolyParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Coeffs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.CoeffsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Lpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.LpcsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.PolyParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.PolyParameter;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.PolyParameterKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.PolyParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.hop.secret.algorithm.type.PolyBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.hop.secret.algorithm.type.poly.PolySecretsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.hop.secret.algorithm.type.poly.poly.secrets.PolySecret;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.hop.secret.algorithm.type.poly.poly.secrets.PolySecretBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ioam.pot.hop.secret.algorithm.type.poly.poly.secrets.PolySecretKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.ProfileIndexRange;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotHopAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.TimeResolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles PoT configuration generation for SFC proof of transit.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.pot.provider.api.SfcPotPolyAPI
 * @since 2016-05-01
 */
public class SfcPotPolyAPI extends SfcPotAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotPolyAPI.class);
    private static final int  DEF_NUM_PROFILES = 2;
    private static final long MIN_NUM_HOPS = 2;

    private java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits;
    private Long refreshPeriodValue;
    private BitMaskOptions ioamPotProfileBitMask;

    private final SfcPotPolyClassAPI sfcPotPolyClassAPI;

    private long opaque;

    public List<SfcPotPolyClass> polyClassList;

    public SfcPotPolyAPI () {
        sfcPotPolyClassAPI = new SfcPotPolyClassAPI();
    }

    /* Utility function to return the next index to use */
    private int getNewActiveIndex (int currActiveIndex) {
        int newIndex = 0;
        int currIndex = currActiveIndex;
        int numProfiles = DEF_NUM_PROFILES;

        currIndex++;
        newIndex = (currIndex % numProfiles);

        return newIndex;
    }

    /*
     * This function is used for initial configuration generation for a specified number
     * of profiles.
     */
    @Override
    public boolean init(String rspName, List<SfName> sfNameList, List<String> sfgNameList,
                        int serviceIndex,
                        final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                        Long           refreshPeriodValue,
                        BitMaskOptions ioamPotProfileBitMask) {

        if (sfNameList == null || sfNameList.size() < MIN_NUM_HOPS) {
            return false;
        }

        int size = sfNameList.size();

        return (init(rspName, size, sfgNameList, serviceIndex,
                     refreshPeriodTimeUnits, refreshPeriodValue, ioamPotProfileBitMask));
    }

    /*
     * This function is used for initial configuration generation for a specified number
     * of profiles.
     */
    @Override
    public boolean init(String rspName, int sfSize, List<String> sfgNameList,
                        int serviceIndex,
                        final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                        Long           refreshPeriodValue,
                        BitMaskOptions ioamPotProfileBitMask) {
        long  prime;
        long  secret;
        long  numProfiles;
        List<Coeffs> coeffs = new ArrayList<>();
        List<Long> shares = new ArrayList<>();
        List<Lpcs> lpcs = new ArrayList<>();

        //
        // Lookup polylist for this rsp and if not present, create one.
        //
        if (polyClassList == null) {
            polyClassList = sfcPotPolyClassAPI.getPolyClassList(rspName);
            if (polyClassList == null) {
                polyClassList = new ArrayList<>();
            }
        }

        this.refreshPeriodTimeUnits = refreshPeriodTimeUnits;
        this.refreshPeriodValue = refreshPeriodValue;
        this.ioamPotProfileBitMask = ioamPotProfileBitMask;

        if (sfSize < MIN_NUM_HOPS) {
            LOG.error("init:Error in input values: size:{}", sfSize);
            return false;
        }

        /* In future, if needed, one can make it variable number of profiles */
        numProfiles = DEF_NUM_PROFILES;

        sfcPotPolyClassAPI.setNumProfiles(numProfiles);

        //Also set the SB profiles information appropriately.
        SfcPotConfigGenerator configGenerator = new SfcPotConfigGenerator(sfSize);
        configGenerator.generateScvConfig();


        for (long j = 0; j < numProfiles; j++) {
            prime = configGenerator.getPrime();
            secret = configGenerator.getSecret();
            for (int i = 1; i < sfSize; i++) {
                coeffs.add(new CoeffsBuilder().setCoeff(configGenerator.getCoeff(i)).build());
                lpcs.add(new LpcsBuilder().setLpc ((configGenerator.getLpc(i)).longValue()).build());
            }
            for (int i = 0; i < sfSize; i++) {
                shares.add(configGenerator.getSecretShare(i));
            }
            polyClassList.add(new SfcPotPolyClass(prime, secret, coeffs, shares, lpcs, (long)sfSize));

            //Re-Initialize containers....
            coeffs = new ArrayList<>();
            shares = new ArrayList<>();
            lpcs = new ArrayList<>();
        }

        //add/update it in a separate memory store
        sfcPotPolyClassAPI.putPolyClassList(rspName, polyClassList);

        return true;
    }


    /*
     * This function is used to re-generate configuration AT the next index,
     * given the current index.  This is used to update the unused profile and
     * then make the nodes use the updated configuration.
     */
    @Override
    public int initRenew(String rspName, int sfSize, List<String> sfgNameList,
                             int serviceIndex, int currActiveIndex) {
        long  prime;
        long  secret;
        List<Coeffs> coeffs = new ArrayList<>();
        List<Long> shares = new ArrayList<>();
        List<Lpcs> lpcs = new ArrayList<>();
        List<SfcPotPolyClass> polyClassListCopy = new ArrayList<>();

        if (sfSize < MIN_NUM_HOPS) {
            LOG.error("initRenew:Error in input values: size:{}", sfSize);
            return -1;
        }

        if (polyClassList == null) {
            polyClassList = sfcPotPolyClassAPI.getPolyClassList(rspName);
            if (polyClassList == null) {
                LOG.error("initRenew:Error in getting parameters for renew:RSP:{}", rspName);
                return -1;
            }
        }

        SfcPotConfigGenerator configGenerator = new SfcPotConfigGenerator(sfSize);
        configGenerator.generateScvConfig();

        boolean ret = polyClassListCopy.addAll(0, polyClassList);
        if (!ret) {
            LOG.warn("initRenew:array list copy error...");
            return -1;
        }

        /* Get the index that needs the config renew. Odd if current is even etc., */
        int newActiveIndex = getNewActiveIndex(currActiveIndex);

        /* Generate the cfg at the renew index */
        prime = configGenerator.getPrime();
        secret = configGenerator.getSecret();
        for (int i = 1; i < sfSize; i++) {
            coeffs.add(new CoeffsBuilder().setCoeff(configGenerator.getCoeff(i)).build());
            lpcs.add(new LpcsBuilder().setLpc ((configGenerator.getLpc(i)).longValue()).build());
        }
        for (int i = 0; i < sfSize; i++) {
            shares.add(configGenerator.getSecretShare(i));
        }

        /* set: overwrites the existing element */
        try {
            polyClassListCopy.set(newActiveIndex, (new SfcPotPolyClass(prime, secret,
                                                               coeffs, shares, lpcs, (long)sfSize)));
        } catch (IndexOutOfBoundsException err) {
            LOG.warn("Index out of bounds: {} ", newActiveIndex, err);
        }

        /* add/update it in a memory store */
        sfcPotPolyClassAPI.putPolyClassList(rspName, polyClassListCopy);

        /* re-fetch */
        polyClassList = sfcPotPolyClassAPI.getPolyClassList(rspName);

        LOG.debug("initRenew:Configuration updated at {}...", newActiveIndex);

        return newActiveIndex;
    }

    @Override
    public boolean setBuilderHopSecret(RenderedServicePathHopBuilder renderedServicePathHopBuilder, int serviceIndex,
                                    short posIndex, SfName serviceFunctionName, ServiceFunction serviceFunction) {
        boolean flagPot = false;
        List<Long> shares;
        long secret;
        long numProfiles = sfcPotPolyClassAPI.getNumProfiles();

        if (polyClassList == null) {
            LOG.error("setBuilderHopSecret: polyClassList null.");
            return false;
        }

        RspIoamPotHopAugmentationBuilder builder = new RspIoamPotHopAugmentationBuilder();

        PolySecretsBuilder polySecretsBuilder = new PolySecretsBuilder();

        ArrayList<PolySecret> polySecretList = new ArrayList<>();

        for (long j = 0; j < numProfiles; j++) {
            PolySecretBuilder polySecretBuilder = new PolySecretBuilder();

            shares = polyClassList.get((int)j).getShares();
            secret = polyClassList.get((int)j).getSecret();

            polySecretBuilder.setSecretShare(shares.get(posIndex))
                             .setPindex(j)
                             .setKey(new PolySecretKey(j));
            if (posIndex == (shares.size() - 1)) {
                polySecretBuilder.setSecret(secret);
                flagPot = true;
            }
            polySecretList.add(polySecretBuilder.build());
        }

        polySecretsBuilder.setPolySecret(polySecretList);

        builder.setAlgorithmType(new PolyBuilder().setPolySecrets(polySecretsBuilder.build()).build());
        renderedServicePathHopBuilder.addAugmentation(RspIoamPotHopAugmentation.class, builder.build());

        return flagPot;
    }


    @Override
    public void setIoamPotParameters(RspIoamPotAugmentationBuilder builder, int currActiveIndex) {
        List<Coeffs> coeffs;
        List<Lpcs> lpcs;
        long prime;
        long numProfiles = sfcPotPolyClassAPI.getNumProfiles();

        if (polyClassList == null) {
            LOG.error("setIoamPotParameters: polyclasslist null.");
            return;
        }

        PolyParametersBuilder polyParamsBuilder = new PolyParametersBuilder();
        polyParamsBuilder.setRefreshPeriodTimeUnits(refreshPeriodTimeUnits)
                         .setRefreshPeriodValue(this.refreshPeriodValue)
                         .setActiveProfileIndex(new ProfileIndexRange(currActiveIndex))
                         .setProfileBitMaskValue(this.ioamPotProfileBitMask);
        ArrayList<PolyParameter> polyParameterList = new ArrayList<>();

        for (long j = 0; j < numProfiles; j++) {
            PolyParameterBuilder polyParameterBuilder = new PolyParameterBuilder();

            coeffs  = polyClassList.get((int)j).getCoeffs();
            lpcs    = polyClassList.get((int)j).getLpcs();
            prime   = polyClassList.get((int)j).getPrime();

            polyParameterBuilder.setPrime(prime)
                                .setCoeffs(coeffs)
                                .setLpcs(lpcs)
                                .setPindex(j)
                                .setKey(new PolyParameterKey(j));

            polyParameterList.add(polyParameterBuilder.build());
        }

        polyParamsBuilder.setPolyParameter(polyParameterList);

        builder.setAlgorithmParameters(new PolyParamsBuilder().setPolyParameters(polyParamsBuilder.build()).build());
    }

    @Override
    public void close() {
    }
}
