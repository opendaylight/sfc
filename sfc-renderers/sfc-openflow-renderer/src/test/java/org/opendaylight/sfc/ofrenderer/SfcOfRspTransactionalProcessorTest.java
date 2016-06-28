/*
 * Copyright (c) 2016 Ericsson Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowWriterImpl;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowWriterInterface;
import org.opendaylight.sfc.ofrenderer.processors.SfcOfRspProcessor;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfProviderUtilsTestMock;
import org.opendaylight.sfc.ofrenderer.utils.SfcSynchronizer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.*;

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
    SfcOfFlowProgrammerInterface flowProgrammerTestMoc;
    SfcOfProviderUtilsTestMock sfcUtilsTestMock;
    List<SftTypeName> sfTypes;
    List<SftTypeName> serviceFunctionChain1, serviceFunctionChain2;
    RenderedServicePath nshRsp, nshRsp2;
    SfcOfFlowWriterInterface sfcFlowWriterTestMock;

    public SfcOfRspTransactionalProcessorTest() {
        LOG.info("SfcOfRspTransactionalProcessorTest constructor");

        this.sfcFlowWriterTestMock = Mockito.spy(new SfcOfFlowWriterImpl());
        this.flowProgrammerTestMoc = Mockito.spy(new SfcOfFlowProgrammerImpl(sfcFlowWriterTestMock));

        Mockito.doNothing().when(sfcFlowWriterTestMock).flushFlows();
        Mockito.doNothing().when(sfcFlowWriterTestMock).deleteFlowSet();

        this.sfcUtilsTestMock = new SfcOfProviderUtilsTestMock();
        // spied in order to check private methods
        this.sfcOfRspProcessor = PowerMockito.spy(new SfcOfRspProcessor(
                this.flowProgrammerTestMoc,
                this.sfcUtilsTestMock,
                new SfcSynchronizer()));

        this.rspBuilder = new RspBuilder(this.sfcUtilsTestMock);
        this.sfTypes = new ArrayList<SftTypeName>();
        this.sfTypes.add(new SftTypeName("firewall"));
        this.sfTypes.add(new SftTypeName("http-header-enrichment"));

        serviceFunctionChain1 = new ArrayList<SftTypeName>();
        serviceFunctionChain2 = new ArrayList<SftTypeName>();
        serviceFunctionChain1.addAll(sfTypes);
        serviceFunctionChain2.add(sfTypes.get(0)); // firewall only
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class);
        this.nshRsp2 = rspBuilder.createRspFromSfTypes(serviceFunctionChain2, VxlanGpe.class);
    }

    @Before
    public void before() throws ExecutionException, InterruptedException {
        LOG.info("SfcOfRspTransactionalProcessorTest before()");
        sfcUtilsTestMock.resetCache();
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
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class);

        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);
        PowerMockito.verifyPrivate(this.sfcOfRspProcessor, times(2)).invoke("setSffInitialized", (Uri) anyObject(), Matchers.eq(true));

        LOG.info("testSingleRSPDeletion: flow creation completed - starting deletion");
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp);
        PowerMockito.verifyPrivate(this.sfcOfRspProcessor, times(2)).invoke("setSffInitialized", (Object) anyObject(), Matchers.eq(false));
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> theMap = Whitebox.getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        assertThat(theMap.size(), is(0));
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
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class);
        this.nshRsp2 = rspBuilder.createRspFromSfTypes(serviceFunctionChain2, VxlanGpe.class);


        // Step 1. Create RSP 1
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap1 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock).flushFlows();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterRsp1 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap1.values()) {
            allFlowsAfterRsp1.addAll(flowsInRsp);
            LOG.debug("  Step 1: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionSymmetrical: After creation of first "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap1.keySet().size(),
                allFlowsAfterRsp1.size());

        // Step 2. Create RSP2
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp2);
        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap2 = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).flushFlows();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterRsp2 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap2.values()) {
            allFlowsAfterRsp2.addAll(flowsInRsp);
            LOG.debug("  Step 2: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionSymmetrical: After creation of second "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap2.keySet().size(),
                allFlowsAfterRsp2.size());

        // Step 3. Delete RSP2. Check that the state afterwards is the expected one
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp2);
        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap3 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock).deleteFlowSet();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterDeletingRsp2 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap3.values()) {
            allFlowsAfterDeletingRsp2.addAll(flowsInRsp);
            LOG.debug("  Step 3: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionSymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap3.keySet().size(),
                allFlowsAfterDeletingRsp2.size());

        Assert.assertEquals(rspToFlowsMap3, rspToFlowsMap1);
        Assert.assertEquals(allFlowsAfterDeletingRsp2.size(), allFlowsAfterRsp1.size());
        // check the flows are exactly the same
        Assert.assertTrue(allFlowsAfterDeletingRsp2.containsAll(allFlowsAfterRsp1)
                          && allFlowsAfterRsp1.containsAll(allFlowsAfterDeletingRsp2));

        // Step 4. Delete RSP1. Check that everything is cleaned up
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp);
        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap4 = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).deleteFlowSet();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterDeletingRsp2And1 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap4.values()) {
            allFlowsAfterDeletingRsp2And1.addAll(flowsInRsp);
            LOG.debug("  Step 4: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionSymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap4.keySet().size(),
                allFlowsAfterDeletingRsp2And1.size());

        Assert.assertEquals(rspToFlowsMap4.size(), 0);
        Assert.assertEquals(allFlowsAfterDeletingRsp2.size(), 0);
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
        this.nshRsp = rspBuilder.createRspFromSfTypes(serviceFunctionChain1, VxlanGpe.class);
        this.nshRsp2 = rspBuilder.createRspFromSfTypes(serviceFunctionChain2, VxlanGpe.class);

        // Step 0(Preparation): first create RSP2, store info of created flows. Then
        // delete RSP2. This is done in order to have the set of flows
        // for RSP 2 only so later we'll be able to compare with the set of flows after C:RSP1-C:RSP2-D:RSP1
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp2);
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> theRspToFlowsMapWithRSP2Only = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterCreatingRSP2Only = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: theRspToFlowsMapWithRSP2Only.values()) {
            allFlowsAfterCreatingRSP2Only.addAll(flowsInRsp);
            LOG.debug("  Preparation: added {} flows to reference flow list (for RSP2)",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionASymmetrical: flow creation completed - starting deletion");
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp2);

        // Step 1. Create RSP 1
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp);

        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap1 = Whitebox.getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).flushFlows();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterRsp1 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap1.values()) {
            allFlowsAfterRsp1.addAll(flowsInRsp);
            LOG.debug("  Step 1: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionASymmetrical: After creation of first "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap1.keySet().size(),
                allFlowsAfterRsp1.size());

        // Step 2. Create RSP2
        this.sfcOfRspProcessor.processRenderedServicePath(nshRsp2);
        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap2 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(3)).flushFlows();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterRsp2 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap2.values()) {
            allFlowsAfterRsp2.addAll(flowsInRsp);
            LOG.debug("  Step 2: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionASymmetrical: After creation of second "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap2.keySet().size(),
                allFlowsAfterRsp2.size());

        // Step 3. Delete RSP1. Check that the state afterwards is the expected one
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp);
        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap3 = Whitebox.
                getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(2)).deleteFlowSet();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterDeletingRsp1 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap3.values()) {
            allFlowsAfterDeletingRsp1.addAll(flowsInRsp);
            LOG.debug("  Step 3: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionASymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap3.keySet().size(),
                allFlowsAfterDeletingRsp1.size());

        // Flows after C:RSP1-C:RSP2-D-RSP1 should be the same than after C:RSP2
        Assert.assertEquals(rspToFlowsMap3.size(), 2);
        Assert.assertEquals(allFlowsAfterDeletingRsp1.size(), allFlowsAfterCreatingRSP2Only.size());
        Assert.assertTrue(allFlowsAfterDeletingRsp1.containsAll(allFlowsAfterCreatingRSP2Only)
                          && allFlowsAfterCreatingRSP2Only.containsAll(allFlowsAfterDeletingRsp1));

        // Step 4. Delete RSP1. Check that everything is cleaned up
        this.sfcOfRspProcessor.deleteRenderedServicePath(nshRsp2);
        // Store the list of flows created at this point
        Map<Long, List<SfcOfFlowWriterImpl.FlowDetails>> rspToFlowsMap4 = Whitebox
                .getInternalState(sfcFlowWriterTestMock, "rspNameToFlowsMap");
        verify(sfcFlowWriterTestMock, times(3)).deleteFlowSet();

        List<SfcOfFlowWriterImpl.FlowDetails> allFlowsAfterDeletingRsp1And2 = new ArrayList<SfcOfFlowWriterImpl.FlowDetails>();
        for (List<SfcOfFlowWriterImpl.FlowDetails> flowsInRsp: rspToFlowsMap4.values()) {
            allFlowsAfterDeletingRsp1And2.addAll(flowsInRsp);
            LOG.debug("  Step 4: added {} flows to flow list",  flowsInRsp.size());
        }
        LOG.debug("testMultiRSPDeletionASymmetrical: After deletion of RSP2: "
                + "RSP: {} RSPs and {} flows stored internally by the flow writer",
                rspToFlowsMap4.keySet().size(),
                allFlowsAfterDeletingRsp1And2.size());

        Assert.assertEquals(rspToFlowsMap4.size(), theRspToFlowsMapWithRSP2Only.size());
        Assert.assertEquals(allFlowsAfterDeletingRsp1And2.size(), 0);
    }

}
