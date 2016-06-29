/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.provider.api;

import java.util.Random;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.GenerationAlgorithmEnum;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ServicePathIds;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ServicePathIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathIdBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathIdKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * This class has the APIs to operate on the Service PathIds.
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * <p>
 * @since 2015-08-09
 */
public class SfcServicePathId {


    /* Initialization value */

    /* We only use half of the path-id space in order to use algorithmic symmetric
     * path-id generation
     */
    private final static int MAX_PATH_ID = (int) Math.pow(2, 12) - 1;
    private final static int MIN_PATH_ID = 0;
    private final static int DEFAULT_GENERATION_ALGORITHM = GenerationAlgorithmEnum.Random.getIntValue();
    private static final Random randomGenerator = new Random();
    private static int next_pathid = 0;
    private final static int num_pathid = MAX_PATH_ID -  MIN_PATH_ID + 1;

    /* Determines the trade-off */
    private final static int N = 64;

    /**
     * Algorithm to randomize the generation of pathIds. Provides
     * security by making path-id less predictable. Adapted
     * from NAT port allocation algorithm
     *
     * <p>
     * @return Pathid or error if none available
     */
    public static int generatePathIdRandomIncrements() {
        int pathid;
        int count = num_pathid;

        do {
            next_pathid = next_pathid + (randomGenerator.nextInt(num_pathid) % N) + 1;
            pathid = MIN_PATH_ID + (next_pathid % num_pathid);

            if (check_suitable_pathid(pathid)) {
                return pathid;
            }

            count--;
        } while (count > 0);

        return -1;
    }

    /**
     * Algorithm to sequentially generate pathIds.
     *
     * <p>
     * @return Pathid or error if none available
     */
    public static int generatePathIdSequentialIncrements() {
        int pathid;
        int count = num_pathid;

        do {
            next_pathid = next_pathid + 1;
            pathid = MIN_PATH_ID + next_pathid;

            if (check_suitable_pathid(pathid) && allocate_pathid(pathid)) {
                return pathid;
            }

            count--;
        } while (count > 0);

        return -1;
    }

    /**
     * Check and allocate Pathid if available
     *
     * <p>
     * @param  pathid Candidate Path Id
     * @return True if allocated, otherwise false.
     */
    public static long check_and_allocate_pathid(long pathid) {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                if (check_suitable_pathid(pathid) && allocate_pathid(pathid)) {
                    return pathid;
                } else {
                    return -1;
                }
            } finally {
                SfcConcurrencyAPI.releasePathIdLock();
            }
        } else {
            return -1;
        }
    }

    /**
     * Check and allocate symmetric Pathid if available
     *
     * <p>
     * @param  pathid Candidate Path Id
     * @return True if allocated, otherwise false.
     */
    public static long check_and_allocate_symmetric_pathid(long pathid) {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                long symmetric_id = -1;
                GenerationAlgorithmEnum genAlg = getGenerationAlgorithm();
                if(genAlg == GenerationAlgorithmEnum.Random) {
                    symmetric_id = pathid ^ (1 << 23);
                    if(!check_suitable_pathid(symmetric_id)) {
                        symmetric_id = -1;
                    }
                } else if(genAlg == GenerationAlgorithmEnum.Sequential) {
                    symmetric_id = generatePathIdSequentialIncrements();
                }

                if ((symmetric_id >= MIN_PATH_ID) && allocate_pathid(symmetric_id)) {
                    return symmetric_id;
                } else {
                    return -1;
                }
            } finally {
                SfcConcurrencyAPI.releasePathIdLock();
            }
        } else {
            return -1;
        }
    }

    /**
     * Generate pathid, check and allocate if available
     *
     * <p>
     * @return True if allocated, otherwise false.
     */
    public static long check_and_allocate_pathid() {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                long pathId = -1;
                GenerationAlgorithmEnum genAlg = getGenerationAlgorithm();
                if(genAlg == GenerationAlgorithmEnum.Random) {
                    pathId = generatePathIdRandomIncrements();
                } else if(genAlg == GenerationAlgorithmEnum.Sequential) {
                    pathId = generatePathIdSequentialIncrements();
                }

                if ((pathId >= MIN_PATH_ID) && allocate_pathid(pathId)) {
                    return pathId;
                } else {
                    return -1;
                }
            } finally {
                SfcConcurrencyAPI.releasePathIdLock();
            }
        } else {
            return -1;
        }
    }

    /**
     * Check if Pathid is available
     *
     * <p>
     * @param  pathid Candidate Path Id
     * @return True if available, otherwise false.
     */
    public static boolean check_suitable_pathid(long pathid) {
        ServicePathIdKey servicePathIdKey = new
                ServicePathIdKey(pathid/Long.SIZE);

        /* Entry into the bitarray */
        long bitEntry = pathid % Long.SIZE;

        InstanceIdentifier<ServicePathId> spIID;
        spIID = InstanceIdentifier.builder(ServicePathIds.class)
                .child(ServicePathId.class, servicePathIdKey)
                .build();
        ServicePathId servicePathId = SfcDataStoreAPI.
                readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        return ((servicePathId == null) || (((1L << (64 - bitEntry)) & servicePathId.getPathIdBitarray()) == 0));
    }

    /**
     * Allocate Pathid
     *
     * <p>
     * @param  pathid Path Id to be allocated
     * @return True if allocated, otherwise false.
     */
    public static boolean allocate_pathid(long pathid) {
        ServicePathIdKey servicePathIdKey = new
                ServicePathIdKey(pathid/Long.SIZE);

        /* Entry into the bitarray */
        long bitEntry = pathid % Long.SIZE;

        long pathIdBitArray = 0;

        InstanceIdentifier<ServicePathId> spIID;
        spIID = InstanceIdentifier.builder(ServicePathIds.class)
                .child(ServicePathId.class, servicePathIdKey)
                .build();
        ServicePathId servicePathId = SfcDataStoreAPI.
                readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        if (servicePathId != null) {
            pathIdBitArray = (servicePathId.getPathIdBitarray() != null) ? servicePathId.getPathIdBitarray() : 0;
        }

        ServicePathIdBuilder servicePathIdBuilder = new ServicePathIdBuilder();

        servicePathIdBuilder.setPathIdBitarray(pathIdBitArray | (1L << (Long.SIZE - bitEntry)));
        servicePathIdBuilder.setKey(servicePathIdKey);

        return SfcDataStoreAPI.
                writeMergeTransactionAPI(spIID, servicePathIdBuilder.build(), LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Free Pathid
     *
     * <p>
     * @param  pathid Path Id to be freed
     * @return True if freed, otherwise false.
     */
    public static boolean free_pathid(long pathid) {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                ServicePathIdKey servicePathIdKey = new
                        ServicePathIdKey(pathid/Long.SIZE);

                /* Entry into the bitarray */
                long bitEntry = pathid % Long.SIZE;

                InstanceIdentifier<ServicePathId> spIID;
                spIID = InstanceIdentifier.builder(ServicePathIds.class)
                        .child(ServicePathId.class, servicePathIdKey)
                        .build();
                ServicePathId servicePathId = SfcDataStoreAPI.
                        readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

                ServicePathIdBuilder servicePathIdBuilder = new ServicePathIdBuilder(servicePathId);
                servicePathIdBuilder.setKey(servicePathIdKey);
                servicePathIdBuilder.setPathIdBitarray(servicePathId.getPathIdBitarray() & ~(1L << (Long.SIZE - bitEntry)));

                return SfcDataStoreAPI.
                        writeMergeTransactionAPI(spIID, servicePathIdBuilder.build(), LogicalDatastoreType.OPERATIONAL);
            } finally {
                SfcConcurrencyAPI.releasePathIdLock();
            }
        }
        return false;
    }

    /**
     * Get the Path-Id Generation-algorithm from the data-store.
     *
     * If its not present, create it with the default value.
     *
     * <p>
     * @return generation-algorithm enum value
     */
    public static GenerationAlgorithmEnum getGenerationAlgorithm() {
        InstanceIdentifier<ServicePathIds> spIID =
                InstanceIdentifier.builder(ServicePathIds.class).build();
        ServicePathIds servicePathIds =
                SfcDataStoreAPI.
                    readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        if(servicePathIds == null) {
            setGenerationAlgorithm(DEFAULT_GENERATION_ALGORITHM);
            return GenerationAlgorithmEnum.forValue(DEFAULT_GENERATION_ALGORITHM);
        }

        GenerationAlgorithmEnum genAlgorithm = servicePathIds.getGenerationAlgorithm();

        if(genAlgorithm == null) {
            setGenerationAlgorithm(DEFAULT_GENERATION_ALGORITHM);
            return GenerationAlgorithmEnum.forValue(DEFAULT_GENERATION_ALGORITHM);
        } else {
            return genAlgorithm;
        }
    }

    /**
     * Set the PathId Generate-algorithm in the data-store.
     *
     * <p>
     * @param genAlgorithm integer value as taken from service-path-id.yang
     * @return True if successful, otherwise false
     */
    public static boolean setGenerationAlgorithm(int genAlgorithm) {
        return setGenerationAlgorithm(GenerationAlgorithmEnum.forValue(genAlgorithm));
    }

    /**
     * Set the PathId Generate-algorithm in the data-store.
     *
     * <p>
     * @param genAlgorithm enum string value as taken from service-path-id.yang
     * @return True if successful, otherwise false
     */
    public static boolean setGenerationAlgorithm(GenerationAlgorithmEnum genAlgorithm) {
        InstanceIdentifier<ServicePathIds> spIID
            = InstanceIdentifier.builder(ServicePathIds.class).build();

        ServicePathIdsBuilder servicePathIdsBuilder = new ServicePathIdsBuilder();
        servicePathIdsBuilder.setGenerationAlgorithm(genAlgorithm);

        return SfcDataStoreAPI.
                writeMergeTransactionAPI(spIID, servicePathIdsBuilder.build(), LogicalDatastoreType.OPERATIONAL);
    }

}
