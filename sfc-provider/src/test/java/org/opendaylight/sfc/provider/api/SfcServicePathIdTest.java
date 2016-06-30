/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.sfc.provider.api;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.GenerationAlgorithmEnum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
    private final LinkedList<Integer> FlippedPathIdList = new LinkedList<>(
            Arrays.asList(8388608, 8388608, 8388671, 8388672, 8388673, 8389631, 8389632, 8389633));

    @Before
    public void init() {
        super.init();
    }

    /**
     * Method: generatePathIdRandomIncrements()
     */
    @Test
    public void testGeneratePathIdRandomIncrements() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertTrue(pathIdSet.add(SfcServicePathId.generatePathIdRandomIncrements()));
        }
        pathIdSet.clear();
    }

    /**
     * Method: check_and_allocate_pathId(long pathId)
     */
    @Test
    public void testCheck_and_allocate_pathIdPathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertNotEquals(-1, SfcServicePathId.check_and_allocate_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
    }

    /**
     * Method: check_and_allocate_symmetric_pathId(long pathId)
     */
    @Test
    public void testCheck_and_allocate_symmetric_pathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertNotEquals(-1, SfcServicePathId.check_and_allocate_symmetric_pathid(pathId));
        }
        for (Integer pathId : FlippedPathIdList) {
            assertFalse(SfcServicePathId.check_suitable_pathid(pathId));
        }
        for (Integer pathId : FlippedPathIdList) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
    }

    /**
     * Method: check_and_allocate_pathId()
     */
    @Test
    public void testCheck_and_allocate_pathId() throws Exception {
        for (int i = 0; i < 20; i++) {
            final int pathId;
            pathId = SfcServicePathId.generatePathIdRandomIncrements();
            assertNotEquals(-1, pathId);
            assertTrue(pathIdSet.add(pathId));
            assertTrue(SfcServicePathId.allocate_pathid(pathId));
        }
        for (Integer pathId : pathIdSet) {
            assertFalse(SfcServicePathId.check_suitable_pathid(pathId));
        }
        for (Integer pathId : pathIdSet) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
        pathIdSet.clear();
    }

    /**
     * Method: allocate_pathId(long pathId)
     */
    @Test
    public void testAllocate_pathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.allocate_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertFalse(SfcServicePathId.check_suitable_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
    }

    /**
     * Method: check_suitable_pathId(long pathId)
     */
    @Test
    public void testCheck_suitable_pathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.check_suitable_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.allocate_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertFalse(SfcServicePathId.check_suitable_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.check_suitable_pathid(pathId));
        }
    }

    /**
     * Method: free_pathId(long pathId)
     */
    @Test
    public void testFree_pathId() throws Exception {
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.allocate_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
        for (Integer pathId : pathIdList) {
            assertTrue(SfcServicePathId.check_suitable_pathid(pathId));
        }
    }

    /**
     * Method: setGenerationAlgorithm(), check_and_allocate_pathId()
     */
    @Test
    public void testSequentialGenerationAlgorithm() throws Exception {
        SfcServicePathId.setGenerationAlgorithm(GenerationAlgorithmEnum.Sequential);
        final long firstPathId = SfcServicePathId.check_and_allocate_pathid();
        final int numPathIds = 20;

        // Verify the pathIds are created sequentially
        for (long pathId = firstPathId + 1; pathId < firstPathId + numPathIds; ++pathId) {
            assertEquals(SfcServicePathId.check_and_allocate_pathid(), pathId);
        }
        // Free the previously allocated pathIds
        for (long pathId = firstPathId; pathId < firstPathId + numPathIds; ++pathId) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
    }

    /**
     * Method: setGenerationAlgorithm(), check_and_allocate_pathId(), check_and_allocate_symmetric_pathId()
     */
    @Test
    public void testSequentialGenerationAlgorithmSymmetric() throws Exception {
        SfcServicePathId.setGenerationAlgorithm(GenerationAlgorithmEnum.Sequential);
        final long firstPathId = SfcServicePathId.check_and_allocate_pathid();
        final int numPathIds = 40;

        // Verify the pathIds are created sequentially both for the normal and the symmetric cases
        for (long pathId = firstPathId + 1; pathId < firstPathId + numPathIds; pathId += 2) {
            long id = SfcServicePathId.check_and_allocate_pathid();
            assertEquals(id, pathId);
            assertEquals(SfcServicePathId.check_and_allocate_symmetric_pathid(id), pathId + 1);
        }
        // Free the previously allocated pathIds
        for (long pathId = firstPathId; pathId < firstPathId + numPathIds + 1; ++pathId) {
            assertTrue(SfcServicePathId.free_pathid(pathId));
        }
    }

}
