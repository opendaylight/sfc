/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
 *
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since 2015-08-09
 */
public final class SfcServicePathId {
    /*
     * We only use half of the path-id space in order to use algorithmic
     * symmetric path-id generation
     */
    private static final int MAX_PATH_ID = (int) Math.pow(2, 12) - 1;
    private static final int MIN_PATH_ID = 0;
    private static final int DEFAULT_GENERATION_ALGORITHM = GenerationAlgorithmEnum.Random.getIntValue();
    private static final Random RANDOM_GENERATOR = new Random();
    private static int nextPathId = 0;
    private static final int NUM_PATH_ID = MAX_PATH_ID - MIN_PATH_ID + 1;

    /* Determines the trade-off */
    private static final int N = 64;

    private SfcServicePathId() {
    }

    /**
     * Algorithm to randomize the generation of pathIds. Provides security by
     * making path-id less predictable. Adapted from NAT port allocation
     * algorithm
     *
     * <p>
     *
     * @return Pathid or error if none available
     */
    public static int generatePathIdRandomIncrements() {
        int pathid;
        int count = NUM_PATH_ID;

        do {
            nextPathId = nextPathId + RANDOM_GENERATOR.nextInt(NUM_PATH_ID) % N + 1;
            pathid = MIN_PATH_ID + nextPathId % NUM_PATH_ID;

            if (checkSuitablePathId(pathid)) {
                return pathid;
            }
            count--;
        }
        while (count > 0);

        return -1;
    }

    /**
     * Algorithm to sequentially generate pathIds.
     *
     * <p>
     *
     * @return Pathid or error if none available
     */
    private static int generatePathIdSequentialIncrements() {
        int pathid;
        int count = NUM_PATH_ID;

        do {
            nextPathId = nextPathId + 1;
            pathid = MIN_PATH_ID + nextPathId;

            if (checkSuitablePathId(pathid) && allocatePathId(pathid)) {
                return pathid;
            }

            count--;
        }
        while (count > 0);

        return -1;
    }

    /**
     * Check and allocate Pathid if available.
     *
     * <p>
     *
     * @param pathid
     *            Candidate Path Id
     * @return True if allocated, otherwise false.
     */
    public static long chechAndAllocatePathId(long pathid) {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                if (checkSuitablePathId(pathid) && allocatePathId(pathid)) {
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
     * Check and allocate symmetric Pathid if available.
     *
     * <p>
     *
     * @param pathid
     *            Candidate Path Id
     * @return True if allocated, otherwise false.
     */
    public static long checkAndAllocateSymmetricPathId(long pathid) {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                long symmetricId = -1;
                GenerationAlgorithmEnum genAlg = getGenerationAlgorithm();
                if (genAlg == GenerationAlgorithmEnum.Random) {
                    symmetricId = pathid ^ 1 << 23;
                    if (!checkSuitablePathId(symmetricId)) {
                        symmetricId = -1;
                    }
                } else if (genAlg == GenerationAlgorithmEnum.Sequential) {
                    symmetricId = generatePathIdSequentialIncrements();
                }

                if (symmetricId >= MIN_PATH_ID && allocatePathId(symmetricId)) {
                    return symmetricId;
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
     * Generate pathid, check and allocate if available.
     *
     * <p>
     *
     * @return True if allocated, otherwise false.
     */
    public static long checkAndAllocatePathId() {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                long pathId = -1;
                GenerationAlgorithmEnum genAlg = getGenerationAlgorithm();
                if (genAlg == GenerationAlgorithmEnum.Random) {
                    pathId = generatePathIdRandomIncrements();
                } else if (genAlg == GenerationAlgorithmEnum.Sequential) {
                    pathId = generatePathIdSequentialIncrements();
                }

                if (pathId >= MIN_PATH_ID && allocatePathId(pathId)) {
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
     * Check if Pathid is available.
     *
     * <p>
     *
     * @param pathid
     *            Candidate Path Id
     * @return True if available, otherwise false.
     */
    public static boolean checkSuitablePathId(long pathid) {
        ServicePathIdKey servicePathIdKey = new ServicePathIdKey(pathid / Long.SIZE);

        /* Entry into the bitarray */
        long bitEntry = pathid % Long.SIZE;

        InstanceIdentifier<ServicePathId> spIID;
        spIID = InstanceIdentifier.builder(ServicePathIds.class).child(ServicePathId.class, servicePathIdKey).build();
        ServicePathId servicePathId = SfcDataStoreAPI.readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        return servicePathId == null || (1L << 64 - bitEntry & servicePathId.getPathIdBitarray()) == 0;
    }

    /**
     * Allocate Pathid.
     *
     * <p>
     *
     * @param pathid
     *            Path Id to be allocated
     * @return True if allocated, otherwise false.
     */
    public static boolean allocatePathId(long pathid) {
        ServicePathIdKey servicePathIdKey = new ServicePathIdKey(pathid / Long.SIZE);

        /* Entry into the bitarray */
        long bitEntry = pathid % Long.SIZE;

        long pathIdBitArray = 0;

        InstanceIdentifier<ServicePathId> spIID;
        spIID = InstanceIdentifier.builder(ServicePathIds.class).child(ServicePathId.class, servicePathIdKey).build();
        ServicePathId servicePathId = SfcDataStoreAPI.readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        if (servicePathId != null) {
            pathIdBitArray = servicePathId.getPathIdBitarray() != null ? servicePathId.getPathIdBitarray() : 0;
        }

        ServicePathIdBuilder servicePathIdBuilder = new ServicePathIdBuilder();

        servicePathIdBuilder.setPathIdBitarray(pathIdBitArray | 1L << Long.SIZE - bitEntry);
        servicePathIdBuilder.withKey(servicePathIdKey);

        return SfcDataStoreAPI.writeMergeTransactionAPI(spIID, servicePathIdBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Free Pathid.
     *
     * <p>
     *
     * @param pathid
     *            Path Id to be freed
     * @return True if freed, otherwise false.
     */
    public static boolean freePathId(long pathid) {
        if (SfcConcurrencyAPI.getPathIdLock()) {
            try {
                ServicePathIdKey servicePathIdKey = new ServicePathIdKey(pathid / Long.SIZE);

                /* Entry into the bitarray */
                long bitEntry = pathid % Long.SIZE;

                InstanceIdentifier<ServicePathId> spIID;
                spIID = InstanceIdentifier.builder(ServicePathIds.class).child(ServicePathId.class, servicePathIdKey)
                        .build();
                ServicePathId servicePathId = SfcDataStoreAPI.readTransactionAPI(spIID,
                        LogicalDatastoreType.OPERATIONAL);

                ServicePathIdBuilder servicePathIdBuilder = new ServicePathIdBuilder(servicePathId);
                servicePathIdBuilder.withKey(servicePathIdKey);
                servicePathIdBuilder
                        .setPathIdBitarray(servicePathId.getPathIdBitarray() & ~(1L << Long.SIZE - bitEntry));

                return SfcDataStoreAPI.writeMergeTransactionAPI(spIID, servicePathIdBuilder.build(),
                        LogicalDatastoreType.OPERATIONAL);
            } finally {
                SfcConcurrencyAPI.releasePathIdLock();
            }
        }
        return false;
    }

    /**
     * Get the Path-Id Generation-algorithm from the data-store.
     *
     * <p>
     * If its not present, create it with the default value.
     *
     * <p>
     *
     * @return generation-algorithm enum value
     */
    private static GenerationAlgorithmEnum getGenerationAlgorithm() {
        InstanceIdentifier<ServicePathIds> spIID = InstanceIdentifier.builder(ServicePathIds.class).build();
        ServicePathIds servicePathIds = SfcDataStoreAPI.readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        if (servicePathIds == null) {
            setGenerationAlgorithm(DEFAULT_GENERATION_ALGORITHM);
            return GenerationAlgorithmEnum.forValue(DEFAULT_GENERATION_ALGORITHM);
        }

        GenerationAlgorithmEnum genAlgorithm = servicePathIds.getGenerationAlgorithm();

        if (genAlgorithm == null) {
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
     *
     * @param genAlgorithm
     *            integer value as taken from service-path-id.yang
     * @return True if successful, otherwise false
     */
    private static boolean setGenerationAlgorithm(int genAlgorithm) {
        return setGenerationAlgorithm(GenerationAlgorithmEnum.forValue(genAlgorithm));
    }

    /**
     * Set the PathId Generate-algorithm in the data-store.
     *
     * <p>
     *
     * @param genAlgorithm
     *            enum string value as taken from service-path-id.yang
     * @return True if successful, otherwise false
     */
    public static boolean setGenerationAlgorithm(GenerationAlgorithmEnum genAlgorithm) {
        InstanceIdentifier<ServicePathIds> spIID = InstanceIdentifier.builder(ServicePathIds.class).build();

        ServicePathIdsBuilder servicePathIdsBuilder = new ServicePathIdsBuilder();
        servicePathIdsBuilder.setGenerationAlgorithm(genAlgorithm);

        return SfcDataStoreAPI.writeMergeTransactionAPI(spIID, servicePathIdsBuilder.build(),
                LogicalDatastoreType.OPERATIONAL);
    }
}
