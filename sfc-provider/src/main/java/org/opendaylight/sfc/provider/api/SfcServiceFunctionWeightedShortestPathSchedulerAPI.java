/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.opendaylight.sfc.provider.topology.SfcProviderGraph;
import org.opendaylight.sfc.provider.topology.SfcProviderTopologyNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ConnectedSffDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.sfc.provider.topology.SfcProviderWeightedGraph;
import org.opendaylight.sfc.provider.topology.SfcProviderTopologyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class implements weighted shortest path scheduling mode.
 * <p>
 *
 * @author Hongjun Ni (hongjun.ni@intel.com)
 *
 * <p>
 * @since 2015-10-23
 */
public class SfcServiceFunctionWeightedShortestPathSchedulerAPI extends SfcServiceFunctionSchedulerAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionWeightedShortestPathSchedulerAPI.class);
    SfcServiceFunctionWeightedShortestPathSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.WeightedShortestPath.class);
    }



    /**
     * This method finds out name of the Service Function closest to
     * Service Function preSfName per serviceFunctionType.
     *
     * <p>
     * @param serviceFunctionType Type of Service Function to find
     * @param preSfName Name of previous Service Function in Service Function Path
     * @param sfcProviderWeightedGraph Topology graph comprised of all the SFs and SFFs
     * @return String Name of the Service Function with type serviceFunctionType
     */
    private SfName getServiceFunctionByType(ServiceFunctionType serviceFunctionType, SfName preSfName,
            SfcProviderWeightedGraph sfcProviderWeightedGraph) {
        SfName sfcProviderTopologyNodeName = null;
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
        int maxTries = sftServiceFunctionNameList.size();

        /* Return null if sftServiceFunctionNameList is empty */
        if (sftServiceFunctionNameList.size() == 0) {
            LOG.debug("No Service Function for {}", serviceFunctionType);
            return null;
        }


        /* Randomly find one instance of serviceFunctionType
         * and return its name if preSfName is null
         */
        if (preSfName == null) {
            /* Randomly find one instance of serviceFunctionType */
            Random rad = new Random();
            int start = rad.nextInt(sftServiceFunctionNameList.size());
            SfcProviderTopologyNode firstHopNode = null;
            while (maxTries > 0) {
                sfcProviderTopologyNodeName = new SfName(sftServiceFunctionNameList.get(start).getName());
                firstHopNode = sfcProviderWeightedGraph.getNode(sfcProviderTopologyNodeName.getValue());
                if (firstHopNode != null) {
                    break;
                } else {
                    LOG.debug("ServiceFunction {} doesn't exist", sfcProviderTopologyNodeName);
                    sfcProviderTopologyNodeName = null;
                    start = (start + 1) % sftServiceFunctionNameList.size();
                    maxTries--;
                }
            }
            LOG.debug("The first ServiceFunction name: {}", sfcProviderTopologyNodeName);
            return sfcProviderTopologyNodeName; //The first hop
        }

        SfcProviderTopologyNode preSfcProviderTopologyNode = sfcProviderWeightedGraph.getNode(preSfName.getValue());

        /* return null if preSfName doesn't exist in sfcProviderWeightedGraph */
        if (preSfcProviderTopologyNode == null) {
            LOG.debug("Node {} doesn't exist", preSfName);
            return null;
        }

        /* Find one instance of serviceFunctionType closest to preSfName */
        int minWeight = Integer.MAX_VALUE;
        sfcProviderTopologyNodeName = null;
        for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
            SfName curSfName = new SfName(sftServiceFunctionName.getName());
            SfcProviderTopologyNode curSfcProviderTopologyNode = sfcProviderWeightedGraph.getNode(curSfName.getValue());
            if (curSfcProviderTopologyNode == null) {
                // curSfName doesn't exist in sfcProviderWeightedGraph, so skip it
                continue;
            }
            //Find out the weighted shortest path
            int curWeight = sfcProviderWeightedGraph.getWeightedShortestPath(preSfName.getValue(), curSfName.getValue());
            if (minWeight > curWeight)
            {
                minWeight = curWeight;
                sfcProviderTopologyNodeName = curSfName;
            }
        }

        /* sfcProviderTopologyNodeName will be null
         * if the next hop can't be found.
         */
        if (sfcProviderTopologyNodeName == null) {
            LOG.debug("Next hop of {} doesn't exist", preSfName);
        }
        return sfcProviderTopologyNodeName;
    }

    /**
     * This method builds a SfcProviderWeightedGraph comprised of
     * all the SFs and SFFs. sfcProviderWeightedGraph will store
     * all the info about vertex/nod, edge and weight.
     *
     * <p>
     * @param sfcProviderWeightedGraph input and output of this method
     * @return void
     */
    private void buildTopologyGraph(SfcProviderWeightedGraph sfcProviderWeightedGraph)
    {
        SfName sfName;
        SffName sffName;
        SffName toSffName;
        int weight;

        /* Add all the ServiceFunction nodes */
        ServiceFunctions sfs =  SfcProviderServiceFunctionAPI.readAllServiceFunctions();
        List<ServiceFunction> serviceFunctionList = sfs.getServiceFunction();
        for (ServiceFunction serviceFunction : serviceFunctionList) {
            sfName = serviceFunction.getName();
            sfcProviderWeightedGraph.addNode(sfName.getValue());
            LOG.debug("Add ServiceFunction: {}", sfName);
        }

        ServiceFunctionForwarders sffs = SfcProviderServiceForwarderAPI.readAllServiceFunctionForwarders();
        List<ServiceFunctionForwarder> serviceFunctionForwarderList = sffs.getServiceFunctionForwarder();

        /* Add edges and node for every ServiceFunctionForwarder */
        for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
            /* Add ServiceFunctionForwarder node */
            sffName = serviceFunctionForwarder.getName();
            sfcProviderWeightedGraph.addNode(sffName.getValue());
            LOG.debug("Add ServiceFunctionForwarder: {}", sffName);

            List<ServiceFunctionDictionary> serviceFunctionDictionaryList = serviceFunctionForwarder.getServiceFunctionDictionary();

            /* Add edge for every ServiceFunction attached
             * to serviceFunctionForwarder
             */
            for (ServiceFunctionDictionary serviceFunctionDictionary : serviceFunctionDictionaryList) {
                sfName = serviceFunctionDictionary.getName();
                weight = serviceFunctionDictionary.getSffSfDataPlaneLocator().getWeight();
                sfcProviderWeightedGraph.addWeightedEdge(sfName.getValue(), sffName.getValue(), weight);
                LOG.debug("Add SF-to-SFF edge: {} => {}, weight: {}", sfName, sffName, weight);
            }

            List<ConnectedSffDictionary> connectedSffDictionaryList = serviceFunctionForwarder.getConnectedSffDictionary();

            /* Add edge for every ServiceFunctionForwarder connected
             * to serviceFunctionForwarder
             */
            for (ConnectedSffDictionary connectedSffDictionary : connectedSffDictionaryList) {
                toSffName = connectedSffDictionary.getName();
                weight = connectedSffDictionary.getSffSffDataPlaneLocator().getWeight();
                sfcProviderWeightedGraph.addWeightedEdge(sffName.getValue(), toSffName.getValue(), weight);
                LOG.debug("Add SFF-to-SFF edge: {} => {}, weight: {}", sffName, toSffName, weight);
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
     * @return List&lt;String&gt; Service Function name list in the shortest path
     */
    @Override
    public List<SfName> scheduleServiceFunctions(ServiceFunctionChain chain, int serviceIndex, ServiceFunctionPath sfp) {
        SfName preSfName = null;
        SfName sfName = null;
        List<SfName> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());
        SfcProviderWeightedGraph sfcProviderWeightedGraph = new SfcProviderWeightedGraph();
        short index = 0;
        Map<Short, SfName> sfpMapping = getSFPHopSfMapping(sfp);

        /* Build topology graph for all the nodes,
         * including every ServiceFunction and ServiceFunctionForwarder
         */
        buildTopologyGraph(sfcProviderWeightedGraph);

        /* Select a SF instance closest to previous hop in SFP
         * for each ServiceFunction type in sfcServiceFunctionList.
         */
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("ServiceFunction name: {}", sfcServiceFunction.getName());
            SfName hopSf = sfpMapping.get(index++);
            if(hopSf != null){
                sfNameList.add(hopSf);
                continue;
            }

                        ServiceFunctionType serviceFunctionType =
                    SfcProviderServiceTypeAPI.readServiceFunctionType(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    sfName = getServiceFunctionByType(serviceFunctionType,
                                                      preSfName,
                                                      sfcProviderWeightedGraph);
                    if (sfName != null) {
                        sfNameList.add(sfName);
                        preSfName = sfName;
                        LOG.debug("Next Service Function: {}", sfName);
                    } else {
                        LOG.error("Couldn't find a reachable SF for ServiceFunctionType: {}", sfcServiceFunction.getType());
                        return null;
                    }
                } else {
                    LOG.debug("No {} Service Function instance", sfcServiceFunction.getName());
                    return null;
                }
            } else {
                LOG.debug("No {} Service Function type", sfcServiceFunction.getName());
                return null;
            }
        }
        return sfNameList;
    }
}
