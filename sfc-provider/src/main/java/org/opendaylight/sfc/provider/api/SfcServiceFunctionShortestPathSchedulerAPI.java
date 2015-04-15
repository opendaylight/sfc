/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.sfc.provider.topology.SfcProviderGraph;
import org.opendaylight.sfc.provider.topology.SfcProviderTopologyNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class implements shortest path scheduling mode.
 * <p/>
 *
 * @author Shuqiang Zhao (shuqiangx.zhao@intel.com)
 * @author Yi Yang (yi.y.yang@intel.com)
 *
 * <p/>
 * @since 2015-03-13
 */
public class SfcServiceFunctionShortestPathSchedulerAPI extends SfcServiceFunctionSchedulerAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionShortestPathSchedulerAPI.class);
    SfcServiceFunctionShortestPathSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.ShortestPath.class);
    }

    /**
     * This method finds out name of the Service Function closest to
     * Service Function preSfName per serviceFunctionType.
     *
     * <p>
     * @param serviceFunctionType Type of Service Function to find
     * @param preSfName Name of previous Service Function in Service Function Path
     * @param sfcProviderGraph Topology graph comprised of all the SFs and SFFs
     * @return String Name of the Service Function with type serviceFunctionType
     */
    private String getServiceFunctionByType(ServiceFunctionType serviceFunctionType, String preSfName, SfcProviderGraph sfcProviderGraph) {
        String sfcProviderTopologyNodeName = null;
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();

        /* Return null if sftServiceFunctionNameList is empty */
        if (sftServiceFunctionNameList.size() == 0) {
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI no Service Function for {}", serviceFunctionType);
            return null;
        }


        /* Randomly find one instance of serviceFunctionType
         * and return its name if preSfName is null
         */
        if (preSfName == null) {
            /* Randomly find one instance of serviceFunctionType */
            Random rad = new Random();
            sfcProviderTopologyNodeName = sftServiceFunctionNameList.get(rad.nextInt(sftServiceFunctionNameList.size())).getName();
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI the first ServiceFunction name: {}", sfcProviderTopologyNodeName);
            return sfcProviderTopologyNodeName; //The first hop
        }

        SfcProviderTopologyNode preSfcProviderTopologyNode = sfcProviderGraph.getNode(preSfName);

        /* return null if preSfName doesn't exist in sfcProviderGraph */
        if (preSfcProviderTopologyNode == null) {
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI node {} doesn't exist", preSfName);
            return null;
        }

        /* Find one instance of serviceFunctionType closest to preSfName */
        int minLength = Integer.MAX_VALUE;
        int length = 0;
        sfcProviderTopologyNodeName = null;
        for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
            String curSfName = sftServiceFunctionName.getName();
            List<SfcProviderTopologyNode> sfcProviderTopologyNodeList = sfcProviderGraph.getShortestPath(preSfName, curSfName);
            length = sfcProviderTopologyNodeList.size();
            if (length <= 1) {
                LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI no path from {} to {}", preSfName, curSfName);
                continue;
            }
            if (minLength > length)
            {
                minLength = length;
                sfcProviderTopologyNodeName = curSfName;
            }
        }

        /* sfcProviderTopologyNodeName will be null
         * if the next hop can't be found.
         */
        if (sfcProviderTopologyNodeName == null) {
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI next hop of {} doesn't exist", preSfName);
        }
        return sfcProviderTopologyNodeName;
    }

    /**
     * This method builds a SfcProviderGraph comprised of
     * all the SFs and SFFs. sfcProviderGraph will store
     * all the info about vertex/node and edge.
     *
     * <p>
     * @param sfcProviderGraph input and output of this method
     * @return void
     */
    private void buildTopologyGraph(SfcProviderGraph sfcProviderGraph)
    {
        String sfName;
        String sffName;
        String toSffName;

        /* Add all the ServiceFunction nodes */
        ServiceFunctions sfs =  SfcProviderServiceFunctionAPI.readAllServiceFunctionsExecutor();
        List<ServiceFunction> serviceFunctionList = sfs.getServiceFunction();
        for (ServiceFunction serviceFunction : serviceFunctionList) {
            sfName = serviceFunction.getName();
            sfcProviderGraph.addNode(sfName);
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI add ServiceFunction: {}", sfName);
        }

        ServiceFunctionForwarders sffs = SfcProviderServiceForwarderAPI.readAllServiceFunctionForwardersExecutor();
        List<ServiceFunctionForwarder> serviceFunctionForwarderList = sffs.getServiceFunctionForwarder();

        /* Add edges and node for every ServiceFunctionForwarder */
        for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
            /* Add ServiceFunctionForwarder node */
            sffName = serviceFunctionForwarder.getName();
            sfcProviderGraph.addNode(sffName);
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI add ServiceFunctionForwarder: {}", sffName);

            List<ServiceFunctionDictionary> serviceFunctionDictionaryList = serviceFunctionForwarder.getServiceFunctionDictionary();

            /* Add edge for every ServiceFunction attached
             * to serviceFunctionForwarder
             */
            for (ServiceFunctionDictionary serviceFunctionDictionary : serviceFunctionDictionaryList) {
                sfName = serviceFunctionDictionary.getName();
                sfcProviderGraph.addEdge(sfName, sffName);
                LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI add sf-sff edge: {} => {}", sfName, sffName);
            }

            List<ConnectedSffDictionary> connectedSffDictionaryList = serviceFunctionForwarder.getConnectedSffDictionary();

            /* Add edge for every ServiceFunctionForwarder connected
             * to serviceFunctionForwarder
             */
            for (ConnectedSffDictionary connectedSffDictionary : connectedSffDictionaryList) {
                toSffName = connectedSffDictionary.getName();
                sfcProviderGraph.addEdge(sffName, toSffName);
                LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI add sff-sff edge: {} => {}", sffName, toSffName);
            }
        }
    }

    /**
     * This method finds out the shortest Service Function Path
     * for the given Service Function Chain chain, any two adjacent
     * Service Functions in this Service Function Path have the
     * shortest distance compared to other two Service Functions
     * with same Service Function Types.
     *
     * <p>
     * @param chain Service Function Chain to render
     * @param serviceIndex Not used currently
     * @return List<String> Service Funtion name list in the shortest path
     */
    public List<String> scheduleServiceFuntions(ServiceFunctionChain chain, int serviceIndex) {
        String preSfName = null;
        String sfName = null;
        List<String> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());
        SfcProviderGraph sfcProviderGraph = new SfcProviderGraph();

        /* Build topology graph for all the nodes,
         * including every ServiceFunction and ServiceFunctionForwarder
         */
        buildTopologyGraph(sfcProviderGraph);

        /* Select a SF instance closest to previous hop in SFP
         * for each ServiceFunction type in sfcServiceFunctionList.
         */
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("SfcServiceFunctionShortestPathSchedulerAPI ServiceFunction name: {}", sfcServiceFunction.getName());

            ServiceFunctionType serviceFunctionType;
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(sfcServiceFunction.getType());
            List<SftServiceFunctionName> sftServiceFunctionNameList =
                new ArrayList<SftServiceFunctionName>();
            if (serviceFunctionType != null) {
                sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    sfName = getServiceFunctionByType(serviceFunctionType,
                                                      preSfName,
                                                      sfcProviderGraph);
                    if (sfName != null) {
                        sfNameList.add(sfName);
                        preSfName = sfName;
                    }
                }
            }

            if (serviceFunctionType == null
                || sftServiceFunctionNameList.isEmpty()
                || sfName == null) {
                LOG.error("Could find a reachable SF for ServiceFuntionType: {}", sfcServiceFunction.getType());
                return null;
            }
        }
        return sfNameList;
    }
}
