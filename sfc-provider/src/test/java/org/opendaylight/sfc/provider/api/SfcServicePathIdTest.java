/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.GenerationAlgorithmEnum;

/**
 * SfcServicePathId Tester.
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 1.0
 * @since 2015-09-01
 */

public class SfcServicePathIdTest extends AbstractSfcRendererServicePathAPITest {

    private final LinkedHashSet<Integer> pathIdSet = new LinkedHashSet<>();
    private final LinkedList<Integer> pathIdList = new LinkedList<>(Arrays.asList(0, 1, 63, 64, 65, 1023, 1024, 1025));
    private final LinkedList<Integer> flippedPathIdList = new LinkedList<>(
            Arrays.asList(8388608, 8388608, 8388671, 8388672, 8388673, 8389631, 8389632, 8389633));

    @Override
    @Before
    public void init() {
        super.init();
    }

    @Test
    public void testGeneratePathIdRandomIncrements() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertTrue(pathIdSet.add(SfcServicePathId.generatePathIdRandomIncrements()));
        }
        pathIdSet.clear();
    }

    @Test
    public void testCheckAndAllocatePathIdPathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertNotEquals(-1, SfcServicePathId.chechAndAllocatePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
    }

    @Test
    public void testCheckAndAllocateSymmetricPathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertNotEquals(-1, SfcServicePathId.checkAndAllocateSymmetricPathId(pathId));
        }
        for (Integer pathId : flippedPathIdList) {
            assertFalse(SfcServicePathId.checkSuitablePathId(pathId));
        }
        for (Integer pathId : flippedPathIdList) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
    }

    @Test
    public void testCheckAndAllocatePathId() throws Exception {
        for (int i = 0; i < 20; i++) {
            final int pathId;
            pathId = SfcServicePathId.generatePathIdRandomIncrements();
            assertNotEquals(-1, pathId);
            assertTrue(pathIdSet.add(pathId));
            assertTrue(SfcServicePathId.allocatePathId(pathId));
        }
        for (Integer pathId : pathIdSet) {
            assertFalse(SfcServicePathId.checkSuitablePathId(pathId));
        }
        for (Integer pathId : pathIdSet) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
        pathIdSet.clear();
    }

    @Test
    public void testAllocatePathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.allocatePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertFalse(SfcServicePathId.checkSuitablePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
    }

    @Test
    public void testCheck_suitable_pathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.checkSuitablePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.allocatePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertFalse(SfcServicePathId.checkSuitablePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.checkSuitablePathId(pathId));
        }
    }

    @Test
    public void testFree_pathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.allocatePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.checkSuitablePathId(pathId));
        }
    }

    @Test
    public void testSequentialGenerationAlgorithm() throws Exception {
        SfcServicePathId.setGenerationAlgorithm(GenerationAlgorithmEnum.Sequential);
        final long firstPathId = SfcServicePathId.checkAndAllocatePathId();
        final int numPathIds = 20;

        // Verify the pathIds are created sequentially
        for (long pathId = firstPathId + 1; pathId < firstPathId + numPathIds; ++pathId) {
            assertEquals(SfcServicePathId.checkAndAllocatePathId(), pathId);
        }
        // Free the previously allocated pathIds
        for (long pathId = firstPathId; pathId < firstPathId + numPathIds; ++pathId) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
    }

    @Test
    public void testSequentialGenerationAlgorithmSymmetric() throws Exception {
        SfcServicePathId.setGenerationAlgorithm(GenerationAlgorithmEnum.Sequential);
        final long firstPathId = SfcServicePathId.checkAndAllocatePathId();
        final int numPathIds = 40;

        // Verify the pathIds are created sequentially both for the normal and
        // the symmetric cases
        for (long pathId = firstPathId + 1; pathId < firstPathId + numPathIds; pathId += 2) {
            long id = SfcServicePathId.checkAndAllocatePathId();
            assertEquals(id, pathId);
            assertEquals(SfcServicePathId.checkAndAllocateSymmetricPathId(id), pathId + 1);
        }
        // Free the previously allocated pathIds
        for (long pathId = firstPathId; pathId < firstPathId + numPathIds + 1; ++pathId) {
            assertTrue(SfcServicePathId.freePathId(pathId));
        }
    }
}
