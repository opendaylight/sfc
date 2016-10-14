/*
 * Copyright (c) 2015 Intel Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

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
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class implements LoadPath aware scheduling mode.
 * <p>
 *
 * @author Dongeun Suh (dongensuh@gmail.com)
 * @author Jaewook Lee (iioiioiio123@korea.ac.kr)
 * @author Hosung Beak (ghlemd@korea.ac.kr)
 * @author Sangheon Pack (shpack@korea.ac.kr)
 * <p>
 * @since 2016-09-10
 */

public class SfcServiceFunctionLoadPathAwareSchedulerAPI extends SfcServiceFunctionSchedulerAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionLoadPathAwareSchedulerAPI.class);
    SfcServiceFunctionLoadPathAwareSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(
                org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.LoadPathAware.class);
    }

    /**
     * This method finds out name of the Service Function which has lowest load (e,g., CPU Utilization)
     * and lower path between Service Function preSfName than path threshold whcich is predefiend value
     * per serviceFunctionType.
     * In this code, we define path threshold to 3.
     *
     * @param serviceFunctionType Type of Service Function to find
     * @param preSfName Name of previous Service Function in Service Function Path
     * @param sfcProviderGraph Topology graph comprised of all the SFs and SFFs
     * @return String Name of the Service Function with type serviceFunctionType
     */
    private SfName getServiceFunctionByType(ServiceFunctionType serviceFunctionType, SfName preSfName,
            SfcProviderGraph sfcProviderGraph) {
        SfName sfcProviderTopologyNodeName = null;
        SfName sfcProviderTopologyNodeName_backup = null;
        List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
 //       int maxTries = sftServiceFunctionNameList.size();

        /* Return null if there are no available instances for the serviceFunctionType */
        if (sftServiceFunctionNameList.size() == 0) {
            LOG.debug("No Service Function for {}", serviceFunctionType);
            return null;
        }

        /*
         * If this is the first SF instance selection round (i.e., preSfName == null),
         * find a SF instance with the lowest CPU utilization and return its name
         */
        if (preSfName == null) {
            SfName sfName = null;
            SfName sftServiceFunctionName = null;
            java.lang.Long preCPUUtilization = java.lang.Long.MAX_VALUE;

            for (SftServiceFunctionName curSftServiceFunctionName : sftServiceFunctionNameList) {
                sfName = new SfName(curSftServiceFunctionName.getName());

                /* Read sfName of the curSftServiceFunctionName */
                ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
                if (serviceFunction == null) {
                    LOG.error("ServiceFunction {} doesn't exist", sfName);
                    continue;
                }

                /* Read service function description monitor information (e.g., CPU utilization) of the sfName */
                SfcSfDescMon sfcSfDescMon = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfName);
                if (sfcSfDescMon == null) {
                    sftServiceFunctionName = sfName;
                    LOG.error("Read monitor information from Data Store failed! serviceFunction: {}", sfName);
                    break;
                }

                java.lang.Long curCPUUtilization =
                    sfcSfDescMon.getMonitoringInfo().getResourceUtilization().getCPUUtilization();

                if (preCPUUtilization > curCPUUtilization) {
                    preCPUUtilization = curCPUUtilization;
                    sftServiceFunctionName = sfName;
                }
            }

            if (sftServiceFunctionName == null) {
            LOG.error("Failed to get one available ServiceFunction for {}", serviceFunctionType.getType());
            }

            SfcProviderTopologyNode firstHopNode;
            sfcProviderTopologyNodeName =  sftServiceFunctionName;
            /*
             * XXX noticed that SfcProviderGraph sometimes refers to SFFs as well so leaving
             * that alone for now until a general discussion
             * about Schedulers can be had.
             */
            firstHopNode = sfcProviderGraph.getNode(sfcProviderTopologyNodeName.getValue());
            LOG.debug("The first ServiceFunction name: {}", sfcProviderTopologyNodeName);
            return sfcProviderTopologyNodeName;
      }

        SfcProviderTopologyNode preSfcProviderTopologyNode = sfcProviderGraph.getNode(preSfName.getValue());

        /* return null if preSfName doesn't exist in sfcProviderGraph */
        if (preSfcProviderTopologyNode == null) {
            LOG.debug("Node {} doesn't exist", preSfName);
            return null;
        }

        /*
         * If this is not the first SF instance selection round (i.e., preSfName != null), find an
         * instance with the lowest CPU utilization among the instances whose hop counts to the previously
         * selected SF instance are less than or equal to Path_threshold (default value is 3) and return its name
         */
        int Path_length;
        int Path_threshold = 3;
        sfcProviderTopologyNodeName = null;
        sfcProviderTopologyNodeName_backup = null;
        java.lang.Long preCPUUtilization = java.lang.Long.MAX_VALUE;
        java.lang.Long preCPUUtilization_backup = java.lang.Long.MAX_VALUE;
        int preLength = Integer.MAX_VALUE;

        for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
            SfName curSfName = new SfName(sftServiceFunctionName.getName());
            SfcProviderTopologyNode curSfcProviderTopologyNode = sfcProviderGraph.getNode(curSfName.getValue());
            if (curSfcProviderTopologyNode == null) {
                // curSfName doesn't exist in sfcProviderGraph, so skip it
                continue;
            }

            /* Get shotestpath length from the preSfName to curSfName*/
            List<SfcProviderTopologyNode> sfcProviderTopologyNodeList =
                    sfcProviderGraph.getShortestPath(preSfName.getValue(), curSfName.getValue());
            Path_length = sfcProviderTopologyNodeList.size()-1;
            LOG.debug("Shortest path length between {} and {} : {}", preSfName, curSfName, Path_length);

            if (Path_length <= 1) {
                LOG.debug("No path from {} to {}", preSfName, curSfName);
                continue;
            }
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(curSfName);
            if (serviceFunction == null) {
                LOG.error("ServiceFunction {} doesn't exist", curSfName);
                continue;
            }

            /* Read ServiceFunctionMonitor information of curSFName */
            SfcSfDescMon sfcSfDescMon = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(curSfName);
            if (sfcSfDescMon == null) {
                LOG.error("Read monitor information from Data Store failed! serviceFunction: {}", curSfName);
                // Use sfName if no sfcSfDescMon is available
                break;
            }

            java.lang.Long curCPUUtilization =
                    sfcSfDescMon.getMonitoringInfo().getResourceUtilization().getCPUUtilization();
            LOG.debug("CPU Utilization of {} is {}", curSfName, curCPUUtilization);
            if (preCPUUtilization> curCPUUtilization && Path_length <= Path_threshold) {
                preCPUUtilization = curCPUUtilization;
                sfcProviderTopologyNodeName = curSfName;
            } else if (Path_length > Path_threshold) {
                if (preLength > Path_length) {
                    preLength = Path_length;
                    sfcProviderTopologyNodeName_backup = curSfName;
                    preCPUUtilization_backup = curCPUUtilization;
                } else if (preLength == Path_length && preCPUUtilization_backup > curCPUUtilization) {
                    sfcProviderTopologyNodeName_backup = curSfName;
                    preCPUUtilization_backup = curCPUUtilization;
                }
            }
        }

        /*
         *
         *
         */

        if (sfcProviderTopologyNodeName == null) {
        sfcProviderTopologyNodeName = sfcProviderTopologyNodeName_backup;
            LOG.debug("Since we cannot find {} of Sevice function within Path_Threshold, we select backup sf", preSfName);
        }

        /*
         * sfcProviderTopologyNodeName will be null
         * if the next hop can't be found.
         */
        if (sfcProviderTopologyNodeName == null) {
            LOG.debug("Next hop of {} doesn't exist", preSfName);
        }
        return sfcProviderTopologyNodeName;
    }


    /**
     * This method builds a SfcProviderGraph comprised of
     * all the SFs and SFFs. sfcProviderGraph will store
     * all the info about vertex/node and edge.
     * <p>
     *
     * @param sfcProviderGraph input and output of this method
     */
    private void buildTopologyGraph(SfcProviderGraph sfcProviderGraph) {
        SfName sfName;
        SffName sffName;
        SffName toSffName;

        /* Add all the ServiceFunction nodes */
        ServiceFunctions sfs = SfcProviderServiceFunctionAPI.readAllServiceFunctions();
        List<ServiceFunction> serviceFunctionList = sfs.getServiceFunction();
        for (ServiceFunction serviceFunction : serviceFunctionList) {
            sfName = serviceFunction.getName();
            sfcProviderGraph.addNode(sfName.getValue());
            LOG.debug("Add ServiceFunction: {}", sfName);
        }

        ServiceFunctionForwarders sffs = SfcProviderServiceForwarderAPI.readAllServiceFunctionForwarders();
        List<ServiceFunctionForwarder> serviceFunctionForwarderList = sffs.getServiceFunctionForwarder();

        /* Add edges and node for every ServiceFunctionForwarder */
        for (ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList) {
            /* Add ServiceFunctionForwarder node */
            sffName = serviceFunctionForwarder.getName();
            sfcProviderGraph.addNode(sffName.getValue());
            LOG.debug("Add ServiceFunctionForwarder: {}", sffName);

            List<ServiceFunctionDictionary> serviceFunctionDictionaryList =
                    serviceFunctionForwarder.getServiceFunctionDictionary();

            /*
             * Add edge for every ServiceFunction attached
             * to serviceFunctionForwarder
             */
            for (ServiceFunctionDictionary serviceFunctionDictionary : serviceFunctionDictionaryList) {
                sfName = serviceFunctionDictionary.getName();
                sfcProviderGraph.addEdge(sfName.getValue(), sffName.getValue());
                LOG.debug("Add SF-to-SFF edge: {} => {}", sfName, sffName);
            }

            List<ConnectedSffDictionary> connectedSffDictionaryList =
                    serviceFunctionForwarder.getConnectedSffDictionary();

            /*
             * Add edge for every ServiceFunctionForwarder connected
             * to serviceFunctionForwarder
             */
            for (ConnectedSffDictionary connectedSffDictionary : connectedSffDictionaryList) {
                toSffName = connectedSffDictionary.getName();
                sfcProviderGraph.addEdge(sffName.getValue(), toSffName.getValue());
                LOG.debug("Add SFF-to-SFF edge: {} => {}", sffName, toSffName);
            }
        }
    }

    /**
     * This method finds out the load and path-aware Service Function Path
     * for the given Service Function Chain. For each SF type in the given
     * chain, this method calls the getServiceFunctionByType method to find
     * an SF instance to be included in the SFP.
     * For the first SF type in the chain, the getServiceFunctionByType method
     * returns a name of the SF instance who has the lowest CPU utilization.
     * From the second SF type in the chain, the getServiceFunctionByType method
     * returns a name of the instance with the lowest CPU utilization among the
     * instances whose hop counts to the previously selected SF instance are less
     * than or equal to Path_threshold (default value is 3).
     * <p>
     *
     * @param chain Service Function Chain to render
     * @param serviceIndex Not used currently
     * @return List&lt;String&gt; Service Function name list in the shortest path
     */
    @Override
    public List<SfName> scheduleServiceFunctions(ServiceFunctionChain chain, int serviceIndex,
            ServiceFunctionPath sfp) {
        SfName preSfName = null;
        SfName sfName;
        List<SfName> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();
        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());
        SfcProviderGraph sfcProviderGraph = new SfcProviderGraph();
        short index = 0;
        Map<Short, SfName> sfpMapping = getSFPHopSfMapping(sfp);

        /*
         * Build topology graph for all the nodes,
         * including every ServiceFunction and ServiceFunctionForwarder
         */
        buildTopologyGraph(sfcProviderGraph);

        /*
         * Select a SF instance closest to previous hop in SFP
         * for each ServiceFunction type in sfcServiceFunctionList.
         */
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.debug("ServiceFunction name: {}", sfcServiceFunction.getName());
            SfName hopSf = sfpMapping.get(index++);
            if (hopSf != null) {
                sfNameList.add(hopSf);
                continue;
            }

            ServiceFunctionType serviceFunctionType =
                    SfcProviderServiceTypeAPI.readServiceFunctionType(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList =
                        serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    sfName = getServiceFunctionByType(serviceFunctionType, preSfName, sfcProviderGraph);
                    if (sfName != null) {
                        sfNameList.add(sfName);
                        preSfName = sfName;
                        LOG.debug("Next Service Function: {}", sfName);
                    } else {
                        LOG.error("Couldn't find a reachable SF for ServiceFunctionType: {}",
                                sfcServiceFunction.getType());
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
