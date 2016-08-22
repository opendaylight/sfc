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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.IoamPotAlgorithmIdentity;
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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.PolyAlg;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.RspIoamPotHopAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev150717.TimeResolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to handle south-bound configuration generation for SFC verification.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.pot.provider.api.SfcPotPolyAPI
 * @since 2016-05-01
 */
public class SfcPotPolyAPI extends SfcPotAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcPotPolyAPI.class);
    private static final Class<? extends IoamPotAlgorithmIdentity> ALGORITHM = PolyAlg.class;
    private static final long DEF_NUM_PROFILES = 2;
    private static final long MAX_NUM_PROFILES = 16;
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

    @Override
    public boolean init(String rspName, int sfSize, List<String> sfgNameList,
                        int serviceIndex,
                        final java.lang.Class<? extends TimeResolution> refreshPeriodTimeUnits,
                        Long           refreshPeriodValue,
                        BitMaskOptions ioamPotProfileBitMask) {
        long  prime;
        long  secret;
        long  numProfiles = 0;
        long  startindex = 0;
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

        int size = sfSize;
        if (size < MIN_NUM_HOPS) {
            LOG.error("init:Error in input values: size:{}", size);
            return false;
        }

        /* In future, if needed, one can make it variable number of profiles */
        numProfiles = DEF_NUM_PROFILES;

        sfcPotPolyClassAPI.setNumProfiles(numProfiles);

        //Also set the SB profiles information appropriately.
        SfcPotConfigGenerator configGenerator = new SfcPotConfigGenerator(size);
        configGenerator.generateScvConfig();


        for (long j = 0; j < numProfiles; j++) {
            prime = configGenerator.getPrime();
            secret = configGenerator.getSecret();
            for (int i = 1; i < size; i++) {
                coeffs.add(new CoeffsBuilder().setCoeff(configGenerator.getCoeff(i)).build());
                lpcs.add(new LpcsBuilder().setLpc ((configGenerator.calculateLpc( i, size)).longValue()).build());
            }
            for (int i = 0; i < size; i++) {
                shares.add(configGenerator.getSecretShare(i));
            }
            polyClassList.add(new SfcPotPolyClass(prime, secret, coeffs, shares, lpcs, (long)size));

            //Re-Initialize containers....
            coeffs = new ArrayList<>();
            shares = new ArrayList<>();
            lpcs = new ArrayList<>();
        }

        //add/update it in a separate memory store
        sfcPotPolyClassAPI.putPolyClassList(rspName, polyClassList);

        return true;
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
                             .setPindex(Long.valueOf(j))
                             .setKey(new PolySecretKey(Long.valueOf(j)));
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
    public void setIoamPotParameters(RspIoamPotAugmentationBuilder builder) {
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
                         .setRefreshPeriodValue(refreshPeriodValue)
                         .setProfileBitMaskValue(ioamPotProfileBitMask);
        ArrayList<PolyParameter> polyParameterList = new ArrayList<>();

        for (long j = 0; j < numProfiles; j++) {
            PolyParameterBuilder polyParameterBuilder = new PolyParameterBuilder();

            coeffs  = polyClassList.get((int)j).getCoeffs();
            lpcs    = polyClassList.get((int)j).getLpcs();
            prime   = polyClassList.get((int)j).getPrime();

            polyParameterBuilder.setPrime(prime)
                                .setCoeffs(coeffs)
                                .setLpcs(lpcs)
                                .setPindex(Long.valueOf(j))
                                .setKey(new PolyParameterKey(Long.valueOf(j)));

            polyParameterList.add(polyParameterBuilder.build());
        }

        polyParamsBuilder.setPolyParameter(polyParameterList);

        builder.setAlgorithmParameters(new PolyParamsBuilder().setPolyParameters(polyParamsBuilder.build()).build());
    }

    @Override
    public void close() {
    }
}
