/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.PolyParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.PolyParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Coeffs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.CoeffsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.Indices;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.algorithm.ext.algorithm.parameters.poly.params.poly.parameters.poly.parameter.IndicesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.hop.secret.algorithm.type.PolyBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.hop.secret.algorithm.type.poly.PolySecretsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.sfcv.hop.secret.algorithm.type.poly.poly.secrets.*;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to handle south-bound configuration generation for SFC verification.
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationPolyAPI
 * @since 2016-05-01
 */
public class SfcVerificationPolyAPI extends SfcVerificationAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcVerificationPolyAPI.class);
    private static final Class<? extends SfcvAlgorithmIdentity> ALGORITHM = PolyAlg.class;
    private static final long MAX_NUM_PROFILES = 16;
    private static final long MIN_NUM_HOPS = 2;

    private long opaque;

    public List<SfcVerificationPolyClass> polyClassList;

    @Override
    public boolean init(String rspName, List<SfName> sfNameList, List<String> sfgNameList,
                        int serviceIndex, long sfcvNumProfiles, long sfcvProfilesValidator) {

        if (sfNameList == null || sfNameList.size() < MIN_NUM_HOPS) {
            return false;
        }

        int size = sfNameList.size();

        return (init(rspName, size, sfgNameList, serviceIndex, sfcvNumProfiles, sfcvProfilesValidator));

    }

    @Override
    public boolean init(String rspName, int sfSize, List<String> sfgNameList,
                        int serviceIndex, long sfcvNumProfiles, long sfcvProfilesValidator) {
        long  prime;
        long  secret;
        long  numprofiles = 0;
        long  startindex = 0;
        ArrayList<Indices> indices = new ArrayList<>();
        List<Coeffs> coeffs = new ArrayList<>();
        List<Long> shares = new ArrayList<>();

        //
        // Lookup polylist for this rsp and if not present, create one.
        //
        if (polyClassList == null) {
            polyClassList = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getPolyClassList(rspName);
            if (polyClassList == null) {
                polyClassList = new ArrayList<>();
            }
        }

        int size = sfSize;
        if (size < MIN_NUM_HOPS) {
            LOG.error("init:Error in input values: size:{}", size);
            return false;
        }

        numprofiles = sfcvNumProfiles;
        if (numprofiles <= 0) {
            numprofiles = 1;
        } else if (numprofiles > MAX_NUM_PROFILES) {
            numprofiles = MAX_NUM_PROFILES;
        }

        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setNumProfiles(numprofiles);
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setStartIndex(0);
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setProfilesValidator(sfcvProfilesValidator);

        //Also set the SB profiles information appropriately.
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setNumSBProfiles(numprofiles);
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setStartSBIndex(0);
        SfcVerificationConfigGenerator configGenerator = new SfcVerificationConfigGenerator(size);
        configGenerator.generateScvConfig();


        for (long j = 0; j < numprofiles; j++) {
            prime = configGenerator.getPrime();
            secret = configGenerator.getSecret();
            for (int i = 1; i < size; i++) {
                coeffs.add(new CoeffsBuilder().setCoeff(configGenerator.getCoeff(i)).build());
            }
            for (int i = 0; i < size; i++) {
                indices.add(new IndicesBuilder().setIndex((int)configGenerator.getServiceIndices(i)).build());
                shares.add(configGenerator.getSecretShare(i));
            }
            polyClassList.add(new SfcVerificationPolyClass(prime, secret, indices, coeffs, shares, (long)size));

            //Re-Initialize containers....
            indices = new ArrayList<>();
            coeffs = new ArrayList<>();
            shares = new ArrayList<>();
        }

        //add/update it in a separate memory store
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().putPolyClassList(rspName, polyClassList);

        return true;
    }


    @Override
    public boolean initRenew(String rspName, List<SfName> sfNameList, List<String> sfgNameList,
                             int serviceIndex, long sfcvStartIndex, long sfcvNumProfiles) {

        if (sfNameList == null || sfNameList.size() < MIN_NUM_HOPS) {
            return false;
        }

        int size = sfNameList.size();

        return (initRenew(rspName, size, sfgNameList, serviceIndex, sfcvStartIndex, sfcvNumProfiles));
    }

    /*
     * On SFCV renew notification from node, renew the profiles requested in the notification and update the profile configurations
     */
    @Override
    public boolean initRenew(String rspName, int sfSize, List<String> sfgNameList,
                             int serviceIndex, long sfcvStartIndex, long sfcvNumProfiles) {
        long  prime;
        long  secret;
        long  count;
        long  sfcsize;
        ArrayList<Indices> indices = new ArrayList<>();
        List<Coeffs> coeffs = new ArrayList<>();
        List<Long> shares = new ArrayList<>();
        long totNumProfiles = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getNumProfiles();
        List<SfcVerificationPolyClass> polyClassListCopy = new ArrayList<>();

        int size = sfSize;
        if (size < MIN_NUM_HOPS) {
            LOG.error("initRenew:Error in input values: size:{}", size);
            return false;
        }

        if ((sfcvStartIndex < 0)  || (sfcvStartIndex > (MAX_NUM_PROFILES-1)) || (sfcvNumProfiles > MAX_NUM_PROFILES)) {
            LOG.error("initRenew:Error in input values: startIndex:{}, Max:{}", sfcvStartIndex, MAX_NUM_PROFILES);
            return false;
        }

        if (polyClassList == null) {
            polyClassList = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getPolyClassList(rspName);
            if (polyClassList == null) {
                LOG.error("initRenew:Error in getting parameters for renew:RSP:{}", rspName);
                return false;
            }
        }

        Long baseStartIndex = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getStartIndex();
        Long baseEndIndex = baseStartIndex + (totNumProfiles - 1);
        if (sfcvStartIndex > baseEndIndex) {
            LOG.error("initRenew:Error in input values:index: startIndex:{}, Max:{}", sfcvStartIndex, baseEndIndex);
            return false;
        }

        if (sfcvNumProfiles > totNumProfiles) {
            LOG.error("initRenew:Error in input values:num Profiles: sfcvNumProfiles:{}, Max:{}", sfcvNumProfiles, totNumProfiles);
            return false;
        }

        LOG.debug("initRenew:Setting index:{}, num:{}, totNum:{}", sfcvStartIndex, sfcvNumProfiles, totNumProfiles);

        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setNumSBProfiles(sfcvNumProfiles);
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setStartSBIndex(sfcvStartIndex);
        SfcVerificationConfigGenerator configGenerator = new SfcVerificationConfigGenerator(size);
        configGenerator.generateScvConfig();

        boolean ret = polyClassListCopy.addAll(0, polyClassList);
        if (ret == false) {
            LOG.warn("initRenew:array list copy error...");
        }


        count=1;
        for (long j = sfcvStartIndex; ; ) {
            prime = configGenerator.getPrime();
            secret = configGenerator.getSecret();
            for (int i = 1; i < size; i++) {
                coeffs.add(new CoeffsBuilder().setCoeff(configGenerator.getCoeff(i)).build());
            }
            for (int i = 0; i < size; i++) {
                indices.add(new IndicesBuilder().setIndex((int)configGenerator.getServiceIndices(i)).build());
                shares.add(configGenerator.getSecretShare(i));
            }

            /* set: overwrites the existing element */
            try {
                polyClassListCopy.set((int)j, (new SfcVerificationPolyClass(prime, secret, indices, coeffs, shares, (long)size)));
            } catch (IndexOutOfBoundsException err) {
                LOG.warn("Index out of bounds: " + err.getMessage());
            }

            /* Re-initialize containers....*/
            indices = new ArrayList<>();
            coeffs = new ArrayList<>();
            shares = new ArrayList<>();

            /* ring buffer */
            j++;
            j = (j % totNumProfiles);

            /* count number of times and quit */
            if (count == sfcvNumProfiles) break;
            count++;
        }

        /* add/update it in a memory store */
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().putPolyClassList(rspName, polyClassListCopy);

        /* re-fetch */
        polyClassList = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getPolyClassList(rspName);

        return true;
    }


    @Override
    public boolean initRefresh(String rspName, List<SfName> sfNameList, List<String> sfgNameList, int serviceIndex,
                               long sfcvStartIndex, long sfcvNumProfiles) {

        if (sfNameList == null || sfNameList.size() < MIN_NUM_HOPS) {
            return false;
        }

        int size = sfNameList.size();

        return (initRefresh(rspName, size, sfgNameList, serviceIndex, sfcvStartIndex, sfcvNumProfiles));

    }

    /*
     * On SFCV refresh notification from node, send refresh (no renew) of the existing profile configurations
     */
    @Override
    public boolean initRefresh(String rspName, int sfSize, List<String> sfgNameList, int serviceIndex,
                               long sfcvStartIndex, long sfcvNumProfiles) {
        long  prime;
        long  secret;
        long  totNumProfiles = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getNumProfiles();

        int size = sfSize;
        if (size < MIN_NUM_HOPS) {
            LOG.error("initRefresh:Error in input values: size:{}", size);
            return false;
        }

        if ((sfcvStartIndex < 0)  || (sfcvStartIndex > (MAX_NUM_PROFILES-1)) || (sfcvNumProfiles > MAX_NUM_PROFILES)) {
            LOG.error("initRefresh:Error in input values: startIndex:{}, Max:{}", sfcvStartIndex, MAX_NUM_PROFILES);
            return false;
        }

        if (polyClassList == null) {
            polyClassList = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getPolyClassList(rspName);
            if (polyClassList == null) {
                LOG.error("initRefresh:Error in getting parameters for renew:RSP:{}", rspName);
                return false;
            }
        }

        Long baseStartIndex = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getStartIndex();
        Long baseEndIndex = baseStartIndex + (totNumProfiles - 1);
        if (sfcvStartIndex > baseEndIndex) {
            LOG.error("initRefresh:Error in input values:index: startIndex:{}, Max:{}", sfcvStartIndex, baseEndIndex);
            return false;
        }

        if (sfcvNumProfiles > totNumProfiles) {
            LOG.error("initRefresh:Error in input values:num Profiles: sfcvNumProfiles:{}, Max:{}", sfcvNumProfiles, totNumProfiles);
            return false;
        }

        LOG.debug("initRefresh: setting idx:{}, num:{}", sfcvStartIndex, sfcvNumProfiles);
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setNumSBProfiles(sfcvNumProfiles);
        SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().setStartSBIndex(sfcvStartIndex);

        return true;
    }


    @Override
    public boolean setBuilderHopSecret(RenderedServicePathHopBuilder renderedServicePathHopBuilder, int serviceIndex,
                                    short posIndex, SfName serviceFunctionName, ServiceFunction serviceFunction) {
        boolean flagVerify = false;
        ArrayList<Indices> indices;
        List<Long> shares;
        long sfcsize = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getStartSBIndex();
        long secret;
        long numprofiles = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getNumSBProfiles();
        long startindex = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getStartSBIndex();
        long count;
        long sfsize;
        long totNumProfiles = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getNumProfiles();

        if (polyClassList == null) {
            LOG.error("setBuilderHopSecret: polyClassList null.");
            return false;
        }

        RspSfcvHopAugmentationBuilder builder = new RspSfcvHopAugmentationBuilder();

        PolySecretsBuilder polySecretsBuilder = new PolySecretsBuilder();

        ArrayList<PolySecret> polySecretList = new ArrayList<>();

        sfsize = polyClassList.get((int)startindex).getSfcSize();

        count=1;
        for (long j = startindex; ; ) {
            PolySecretBuilder polySecretBuilder = new PolySecretBuilder();

            indices = polyClassList.get((int)j).getIndices();
            shares = polyClassList.get((int)j).getShares();
            secret = polyClassList.get((int)j).getSecret();

            polySecretBuilder.setSecretShare(shares.get(posIndex))
                             .setIndex(indices.get(posIndex).getIndex())
                             .setPindex(new Long(j))
                             .setKey(new PolySecretKey(new Long(j)));
            if (posIndex == (shares.size() - 1)) {
                polySecretBuilder.setSecret(secret);
                flagVerify = true;
            }
            polySecretList.add(polySecretBuilder.build());

            /* ring buffer */
            j++;
            j = (j % totNumProfiles);

            /* count number of times and quit */
            if (count == numprofiles) break;
            count++;
        }

        polySecretsBuilder.setNumPolySecret(numprofiles);
        polySecretsBuilder.setPolySecret(polySecretList);

        builder.setAlgorithmType(new PolyBuilder().setPolySecrets(polySecretsBuilder.build()).build());
        renderedServicePathHopBuilder.addAugmentation(RspSfcvHopAugmentation.class, builder.build());

        return flagVerify;
    }


    @Override
    public void setSfcvParameters(RspSfcvAugmentationBuilder builder, NodeId refreshNode) {
        ArrayList<Indices> indices;
        List<Long> shares;
        List<Coeffs> coeffs;
        long prime;
        long numprofiles = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getNumSBProfiles();
        long basenumprofiles = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getNumProfiles();
        long startindex = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getStartSBIndex();
        long renewttl = SfcVerificationPolyClassAPI.getSfcVerificationPolyClassAPI().getProfilesValidator();
        long count=0;

        if (polyClassList == null) {
            LOG.error("setSfcvParameters: polyclasslist null.");
            return;
        }

        PolyParametersBuilder polyParamsBuilder = new PolyParametersBuilder();
        ArrayList<PolyParameter> polyParameterList = new ArrayList<>();

        count=1;
        for (long j = startindex; ; ) {
            PolyParameterBuilder polyParameterBuilder = new PolyParameterBuilder();

            shares  = polyClassList.get((int)j).getShares();
            indices = polyClassList.get((int)j).getIndices();
            coeffs  = polyClassList.get((int)j).getCoeffs();
            prime   = polyClassList.get((int)j).getPrime();

            polyParameterBuilder.setPrime(prime)
                                .setCoeffs(coeffs)
                                .setIndices(indices)
                                .setMask(56)
                                .setPindex(new Long(j))
                                .setKey(new PolyParameterKey(new Long(j)));

            polyParameterList.add(polyParameterBuilder.build());

            /* ring buffer */
            j++;
            j = (j % basenumprofiles);

            /* count number of times and quit */
            if (count == numprofiles) break;
            count++;
        }

        polyParamsBuilder.setStartIndex(startindex);
        polyParamsBuilder.setBaseNumProfiles(basenumprofiles);
        polyParamsBuilder.setNumPolyParameter(numprofiles);
        polyParamsBuilder.setProfilesValidator(renewttl);
        if (refreshNode != null) {
            polyParamsBuilder.setRefreshNode(refreshNode.toString());
        } else {
            polyParamsBuilder.setRefreshNode(new String(""));
        }
        polyParamsBuilder.setPolyParameter(polyParameterList);

        builder.setAlgorithmParameters(new PolyParamsBuilder().setPolyParameters(polyParamsBuilder.build()).build());
    }

    @Override
    public void close() {
    }
}
