/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import org.mockito.Matchers;
import org.mockito.Mockito;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.util.openflow.OpenflowConstants;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.util.openflow.transactional_writer.SfcOfFlowWriterInterface;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtilsTestMock;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Nsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.opendaylight.sfc.util.openflow.transactional_writer.FlowDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit testing for the complete process of adding several Rendered Service Paths (RSPs), then
 * deleting them (in different orders).
 * After SfcOfRspProcessor receives a RSP, it transactionally creates a set of OpenFlow flows.
 * On this test, only the commit part is mocked, but several checks are performed in order
 * to verify that the expected groups of flows are created / deleted at each
 * step during the creation and deletion of several RSPs
 */

/**
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 * @since 2016-05-26
 */

// We need PowerMock in order to be able to test FlowWriter internal state (private
// methods / lists of flows scheduled to be created / deleted at RSP creation time)
@RunWith(PowerMockRunner.class)
@PrepareForTest(SfcOfRspProcessor.class)
public class SfcOfRspTransactionalProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfRspTransactionalProcessorTest.class);
    SfcOfRspProcessor sfcOfRspProcessor;
    RspBuilder rspBuilder;
    SfcOfFlowProgrammerInterface flowProgrammerTestMock;
    SfcOfProviderUtilsTestMock sfcUtilsTestMock;
    List<SftTypeName> sfTypes;
    List<SftTypeName> serviceFunctionChain1, serviceFunctionChain2;
    RenderedServicePath nshRsp, nshRsp2;
    SfcOfFlowWriterInterface sfcFlowWriterTestMock;
    DataBroker  dataBroker;

    public SfcOfRspTransactionalProcessorTest() {
        LOG.info("SfcOfRspTransactionalProcessorTest constructor");

        this.sfcFlowWriterTestMock = Mockito.spy(new SfcOfFlowWriterImpl());
        this.flowProgrammerTestMock = Mockito.spy(new SfcOfFlowProgrammerImpl(sfcFlowWriterTestMock));

        Mockito.doNothing().when(sfcFlowWriterTestMock).flushFlows();
        Mockito.doNothing().when(sfcFlowWriterTestMock).deleteFlowSet();

        dataBroker = Mockito.mock(DataBroker.class);
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(Mockito.mock(WriteTransaction.class));

        this.sfcUtilsTestMock = new SfcOfProviderUtilsTestMock();
        // spied in order to check private methods
        this.sfcOfRspProcessor = PowerMockito.spy(
                new SfcOfRspProcessor(
                    this.flowProgrammerTestMock,
                    this.sfcUtilsTestMock,
                    new SfcSynchronizer(),
                    null,
                    dataBroker
                    ));



        this.rspBuilder = new RspBuilder(this.sfcUtilsTestMock);
        this.sfTypes = new ArrayList<>();
        this.sfTypes.add(new SftTypeName("firewall"));
        this.sfTypes.add(new SftTypeName("http-header-enrichment"));

        serviceFunctionChain1 = new ArrayList<>();
        serviceFunctionChain2 = new ArrayList<>();
        serviceFunctionChain1.addAll(sfTypes);
        serviceFunctionChain2.add(sfTypes.get(0)); // firewall only
    }

    @Before
    public void before() throws ExecutionException, InterruptedException {
        LOG.info("SfcOfRspTransactionalProcessorTest before()");
        sfcUtilsTestMock.resetCache();
    }

    /**
     * Helper function to check if the SFFs are free of initialization flows or still have them
     *
     * @param theMap the cache of installed flows per sff, per RSP
     * @return true if all SFFs are free of initialization flows, false otherwise
     */
    private boolean
    areSffsFreeOfInitializationFlows(Map<Long, Map<String, List<FlowDetails>>> theMap) {
        Predicate<Map.Entry<String, List<FlowDetails>>> emptySff =
                theInputEntry -> theInputEntry.getValue().size() == 0;
        return theMap.get(OpenflowConstants.SFC_FLOWS).entrySet().stream().allMatch(emptySff);
    }

    /**
     * Attempt to delete an RSP that does not exist
     */
    @Test
    public void deleteNonExistentRsp() {
        sfcFlowWriterTestMock.deleteRspFlows((long) 31);
        Set<FlowDetails> flowsToDelete = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "setOfFlowsToDelete");
        Assert.assertTrue(flowsToDelete.isEmpty());
    }

    @Test
    public void clearNonExistentInitializationFlows() {
        Set<NodeId> theOrphanSffs = sfcFlowWriterTestMock.clearSffsIfNoRspExists();
        Assert.assertTrue(theOrphanSffs.isEmpty());
    }

    /*
     * Create RSP, then delete it.
     *   - Check that all SFFs are returned to the "uninitialized" state
     *   - Check that all RSPs are removed from the flowWriter tables
     */
    @Test
    public void testSingleRSPDeletion() throws Exception {
        LOG.info("testSingleRSPDeletion");

        // needs to be done inside the test(it is cleared in before() method between tests)
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class, Nsh.class);
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        PowerMockito.verifyPrivate(
                this.sfcOfRspProcessor, times(2)).invoke("setSffInitialized", anyObject(), Matchers.eq(true));

        LOG.info("testSingleRSPDeletion: flow creation completed - starting deletion");
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp);

        PowerMockito.verifyPrivate(
                this.sfcOfRspProcessor, times(2)).invoke("setSffInitialized", anyObject(), Matchers.eq(false));

        // fetch the list of flows to be deleted
        Set<FlowDetails> deletedFlows = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "setOfFlowsToDelete");

        // fetch the flow cache
        Map<Long, Map<String, List<FlowDetails>>> theMap =
                Whitebox.getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");

        // all the initialization flows belong to a dummy RSP - check SfcOfRspProcessor SFC_FLOWS constant
        // we won't delete this entry from the cache
        assertThat(theMap.entrySet().size(), is(1));

        // get all the current flows
        Set<FlowDetails> currentFlows =
                theMap.entrySet().stream()
                        .flatMap(rspEntry -> rspEntry.getValue().entrySet().stream())
                        .flatMap(sffEntry -> sffEntry.getValue().stream())
                        .collect(Collectors.toSet());

        // assure that the difference between them is an empty set
        assertThat(Sets.difference(currentFlows, deletedFlows), is(Collections.emptySet()));

        // assure that the SFFs don't have any initialization flows
        Assert.assertTrue(areSffsFreeOfInitializationFlows(theMap));
    }

    /*
     * Create RSP 1, store flow count. Create RSP 2, store flows. Delete RSP2, check that
     * flows are the same than after creation of RSP1
     * Then Delete RSP1, check that all flows are gone
     * In summary: C:RSP1 C:RSP2 D:RSP2 D:RSP1, (deletion order symmetrical to creation order) checking correctness
     */
    @Test
    public void testMultiRSPDeletionSymmetrical() throws Exception {
        LOG.info("testMultiRSPDeletionSymmetrical: starting");

        // needs to be done inside the test(it is cleared in before() method between tests)
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class, Nsh.class);
        this.nshRsp2 = rspBuilder.createRspFromSfTypes(serviceFunctionChain2, VxlanGpe.class, Nsh.class);

        // ---------------------
        // Step 1. Create RSP 1
        // ---------------------
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap1 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock).flushFlows();

        Set<FlowDetails> allFlowsAfterRsp1 = new HashSet<>();

        for (Map<String, List<FlowDetails>> flowsInRsp: rspToFlowsMap1.values()) {
            flowsInRsp.values().forEach(theFlows -> {
                allFlowsAfterRsp1.addAll(theFlows);
            });

            LOG.debug("  Step 1: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionSymmetrical: After creation of first "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap1.keySet().size(),
                allFlowsAfterRsp1.size());

        // ---------------------
        // Step 2. Create RSP2
        // ---------------------
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp2);
        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap2 = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).flushFlows();

        Set<FlowDetails> allFlowsAfterRsp2 = new HashSet<>();
        for (Map<String, List<FlowDetails>> flowsInRsp: rspToFlowsMap2.values()) {
            flowsInRsp.values().forEach( theFlows -> {
                allFlowsAfterRsp2.addAll(theFlows);
            });
            LOG.debug("  Step 2: added {} flows to flow list",  flowsInRsp.size());
        }

        LOG.debug("testMultiRSPDeletionSymmetrical: After creation of second "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap2.keySet().size(),
                allFlowsAfterRsp2.size());

        // ---------------------
        // Step 3. Delete RSP2. Check that the state afterwards is the expected one
        // ---------------------
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp2);

        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap3 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock).deleteFlowSet();

        Set<FlowDetails> deletedFlowsRsp2 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "setOfFlowsToDelete");

        LOG.debug("testMultiRSPDeletionSymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap3.keySet().size(),
                allFlowsAfterRsp2.size() - deletedFlowsRsp2.size());

        Assert.assertEquals(rspToFlowsMap3, rspToFlowsMap1);
        Assert.assertEquals(allFlowsAfterRsp1.size(), allFlowsAfterRsp2.size() - deletedFlowsRsp2.size());
        Assert.assertTrue(Sets.difference(allFlowsAfterRsp2, deletedFlowsRsp2).containsAll(allFlowsAfterRsp1));

        // ---------------------
        // Step 4. Delete RSP1. Check that everything is cleaned up
        // ---------------------
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp);

        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap4 = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).deleteFlowSet();

        // get the list of deleted flows belonging to RSP1
        Set<FlowDetails> allDeletedFlows = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "setOfFlowsToDelete");

        LOG.debug("testMultiRSPDeletionSymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap4.keySet().size(),
                allDeletedFlows.size());

        // assure that we have the right number of flows
        Assert.assertEquals(0, allFlowsAfterRsp2.size() - allDeletedFlows.size());

        // assure that the SFFs don't have any initialization flows
        Assert.assertTrue(areSffsFreeOfInitializationFlows(rspToFlowsMap4));
    }

    /*
     * Create RSP 1, store flow count + flows themselves. Create RSP 2, store flows. Delete RSP1,
     * check that flows are the same than after creation of RSP2 alone
     * Finally Delete RSP2, check that all flows are gone
     * In summary: C:RSP1 C:RSP2 D:RSP1 D:RSP2, checking correctness along the way
     */
    @Test
    public void testMultiRSPDeletionASymmetrical() throws Exception {

        LOG.info("testMultiRSPDeletionASymmetrical - starting");

        // needs to be done inside the test(it is cleared in before() method between tests)
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class, Nsh.class);
        this.nshRsp2 = rspBuilder.createRspFromSfTypes(serviceFunctionChain2, VxlanGpe.class, Nsh.class);

        // ---------------------------------------------------------------------------------------------------------
        // Step 0(Preparation): first create RSP2, store info of created flows. Then delete RSP2.
        // This is done in order to have the set of flows for RSP 2 only, so that we can compare
        // with the set of flows after C:RSP1-C:RSP2-D:RSP1 (end of Step 3)
        // ---------------------------------------------------------------------------------------------------------
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp2);
        Map<Long, Map<String, List<FlowDetails>>> currentRspCache = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");

        Map<Long, Map<String, List<FlowDetails>>> theRspToFlowsMapWithRSP2Only =
                new HashMap<>(currentRspCache);

        Set<FlowDetails> allFlowsAfterCreatingRSP2Only = new HashSet<>();
        for (Map<String, List<FlowDetails>> flowsInRsp: theRspToFlowsMapWithRSP2Only.values()) {
            flowsInRsp.values().forEach(theFlows -> {
                allFlowsAfterCreatingRSP2Only.addAll(theFlows);
            });
        }

        LOG.debug("  Preparation: added {} flows to reference flow list (for RSP2)",
                allFlowsAfterCreatingRSP2Only.size());

        LOG.debug("testMultiRSPDeletionASymmetrical: flow creation completed - starting deletion");
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp2);

        // got to reset the internal buffer of the writer
        sfcFlowWriterTestMock.purgeFlows();
        currentRspCache.clear();

        // ---------------------
        // Step 1. Create RSP 1
        // ---------------------
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap1 =
                Whitebox.getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");

        verify(sfcFlowWriterTestMock, times(2)).flushFlows();

        Set<FlowDetails> allFlowsAfterRsp1 = new HashSet<>();
        for (Map<String, List<FlowDetails>> flowsInRsp: rspToFlowsMap1.values()) {
            flowsInRsp.values().forEach(theFlows -> {
                allFlowsAfterRsp1.addAll(theFlows);
            });
            LOG.debug("  Step 1: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionASymmetrical: After creation of first "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap1.keySet().size(),
                allFlowsAfterRsp1.size());

        // ---------------------
        // Step 2. Create RSP2
        // ---------------------
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp2);
        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap2 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(3)).flushFlows();

        Set<FlowDetails> allFlowsAfterRsp2 = new HashSet<>();
        for (Map<String, List<FlowDetails>> flowsInRsp: rspToFlowsMap1.values()) {
            flowsInRsp.values().forEach(theFlows -> {
                allFlowsAfterRsp2.addAll(theFlows);
            });
            LOG.debug("  Step 1: added {} flows to flow list",  flowsInRsp.size());
        }

        LOG.debug("testMultiRSPDeletionASymmetrical: After creation of second "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap2.keySet().size(),
                allFlowsAfterRsp2.size());

        // ---------------------
        // Step 3. Delete RSP1. Check that the state afterwards is the expected one
        // ---------------------
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp);

        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap3 = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).deleteFlowSet();

        Set<FlowDetails> deletedFlowsRsp1 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "setOfFlowsToDelete");

        LOG.debug("testMultiRSPDeletionSymmetrical: After deletion of RSP2: "
                        + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap3.keySet().size(),
                allFlowsAfterRsp2.size() - deletedFlowsRsp1.size());

        // Flows after C:RSP1-C:RSP2-D-RSP1 should be the same than after C:RSP2
        Assert.assertEquals(2, rspToFlowsMap3.size());
        Assert.assertEquals(rspToFlowsMap3, rspToFlowsMap1);
        Assert.assertEquals(allFlowsAfterCreatingRSP2Only.size(), allFlowsAfterRsp2.size() - deletedFlowsRsp1.size());

        LOG.debug("testMultiRSPDeletionASymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap3.keySet().size(),
                deletedFlowsRsp1.size());

        // ---------------------
        // Step 4. Delete RSP1. Check that everything is cleaned up
        // ---------------------
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp2);

        // Store the list of flows created at this point
        Map<Long, Map<String, List<FlowDetails>>> rspToFlowsMap4 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(3)).deleteFlowSet();

        Set<FlowDetails> deleteAllFlows =
                Whitebox.getInternalState(sfcFlowWriterTestMock, "setOfFlowsToDelete");

        LOG.debug("testMultiRSPDeletionASymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap4.keySet().size(),
                deleteAllFlows.size());

        // assure we have deleted the right number of flows
        Assert.assertEquals(0, allFlowsAfterRsp2.size() - deleteAllFlows.size());

        // assure that the SFFs don't have any initialization flows
        Assert.assertTrue(areSffsFreeOfInitializationFlows(rspToFlowsMap4));
    }
}
