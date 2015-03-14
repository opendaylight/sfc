/*
* Copyright (c) 2014 Intel Corporation. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.sfc.provider.topology;

import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionForwardersDictionary;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Collections;

public class SfcProviderGraph {

    private static Map<SfcProviderTopologyNode, Set<SfcProviderTopologyNode>> sfcProviderTopologyEdges = new HashMap<SfcProviderTopologyNode, Set<SfcProviderTopologyNode>>();
    private static List<SfcProviderTopologyNode> sfcProviderTopologyNodeList = new ArrayList<SfcProviderTopologyNode>();

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderGraph.class);
    private static final int WHITE     = 2;
    private static final int GRAY     = 1;
    private static final int BLACK    = 0;
    private static final int MAX    = 10000;

    private static boolean getServiceFunctionForwarders4Topology()
    {
        ServiceFunctionForwarders sffs = SfcProviderServiceForwarderAPI.readAllServiceFunctionForwardersExecutor();
        List<ServiceFunctionForwarder> serviceFunctionForwarderList = sffs.getServiceFunctionForwarder();

        for(ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList){
            SfcProviderTopologyNode sfcProviderTopologyNode = new SfcProviderTopologyNode();
            sfcProviderTopologyNode.name = serviceFunctionForwarder.getName();

            sfcProviderTopologyNodeList.add(sfcProviderTopologyNode);
            LOG.debug("SfcProviderTopologyNode SFF inserted: {}", sfcProviderTopologyNode.name);
        }

        return true;
    }

    private static boolean getServiceFunctions4Topology()
    {
        ServiceFunctions sfs =  SfcProviderServiceFunctionAPI.readAllServiceFunctionsExecutor();
        List<ServiceFunction> serviceFunctionList = sfs.getServiceFunction();

        for(ServiceFunction serviceFunction : serviceFunctionList)
        {
            SfcProviderTopologyNode sfcProviderTopologyNode = new SfcProviderTopologyNode();
            sfcProviderTopologyNode.name = serviceFunction.getName();
            sfcProviderTopologyNodeList.add(sfcProviderTopologyNode);
        }

        return true;
    }

    private static boolean getEdges4Topology()
    {
        ServiceFunctionForwarders sffs = SfcProviderServiceForwarderAPI.readAllServiceFunctionForwardersExecutor();
        List<ServiceFunctionForwarder> serviceFunctionForwarderList = sffs.getServiceFunctionForwarder();

        java.lang.String sfName = "";
        java.lang.String sffName = "";

        for(ServiceFunctionForwarder serviceFunctionForwarder : serviceFunctionForwarderList){
            // add SFF-SF
            List<ServiceFunctionDictionary> serviceFunctionDictionaryList = serviceFunctionForwarder.getServiceFunctionDictionary();
            for (ServiceFunctionDictionary serviceFunctionDictionary : serviceFunctionDictionaryList){
                sfName = serviceFunctionDictionary.getName();

                // Notice :if sf linked to sff, but not found in getallsfs!! todo something to topo!
                insertEdge(getSingleSfcProviderTopologyNode(serviceFunctionForwarder.getName()), getSingleSfcProviderTopologyNode(sfName));
                insertEdge(getSingleSfcProviderTopologyNode(sfName), getSingleSfcProviderTopologyNode(serviceFunctionForwarder.getName()));
            }

            // add SFF-SFF
            List<ServiceFunctionForwardersDictionary> serviceFunctionForwardersDictionaryList = serviceFunctionForwarder.getServiceFunctionForwardersDictionary();
            for (ServiceFunctionForwardersDictionary serviceFunctionForwardersDictionary : serviceFunctionForwardersDictionaryList){
                sffName = serviceFunctionForwardersDictionary.getName();
                insertEdge(getSingleSfcProviderTopologyNode(serviceFunctionForwarder.getName()), getSingleSfcProviderTopologyNode(sffName));
            }
        }

        return true;
    }

    public static boolean sfcProviderTopologyBuilder()
    {

        if (!getServiceFunctions4Topology()){
            LOG.error("Get All ServiceFunctions for Topology failed!");
            return false;
        }

        if (!getServiceFunctionForwarders4Topology()){
            LOG.error("Get All Service Function Forwarders for Topology failed!");
            return false;
        }

        if (!getEdges4Topology()){
            LOG.error("Get Edges for Topology failed!");
            return false;
        }

        show();
        return true;
    }

    public static SfcProviderTopologyNode getSingleSfcProviderTopologyNode(String sfcProviderTopologyNodeName)
    {
        for (SfcProviderTopologyNode sfcProviderTopologyNode : sfcProviderTopologyNodeList)
        {
            LOG.debug("SfcProviderTopology getSingleSfcProviderTopologyNode : {}", sfcProviderTopologyNode.name);
            if (sfcProviderTopologyNodeName.equals(sfcProviderTopologyNode.name))
                return sfcProviderTopologyNode;
        }

        LOG.debug("SfcProviderTopology getSingleSfcProviderTopologyNode null for: {}", sfcProviderTopologyNodeName);
        return null;
    }

    public static List<SfcProviderTopologyNode> getAllSfcProviderTopologyNode()
    {
        return sfcProviderTopologyNodeList;
    }

    public static void show()
    {
        for (SfcProviderTopologyNode sfcProviderTopologyNode : sfcProviderTopologyEdges.keySet())
        {
            LOG.debug("SfcProviderTopologyNode Edge key: {}", sfcProviderTopologyNode.name);
            for (SfcProviderTopologyNode sfcNode : sfcProviderTopologyEdges.get(sfcProviderTopologyNode))
            {
                LOG.debug("SfcProviderTopologyNode attached Edge : {}", sfcNode.name);
            }
        }
    }

    private static void insertEdge(SfcProviderTopologyNode preSfcNode, SfcProviderTopologyNode curSfcNode)
    {
        Set<SfcProviderTopologyNode> sfcProviderTopologyNodeSet = sfcProviderTopologyEdges.get(preSfcNode);
        if ( sfcProviderTopologyNodeSet == null){
            sfcProviderTopologyNodeSet = new HashSet<SfcProviderTopologyNode>();
        }

        sfcProviderTopologyNodeSet.add(curSfcNode);
        sfcProviderTopologyEdges.put(preSfcNode, sfcProviderTopologyNodeSet);
    }

    private static void breadthFirstSearch(SfcProviderTopologyNode sfcProviderTopologyNode)
    {
        for (SfcProviderTopologyNode sfcNode : sfcProviderTopologyEdges.keySet())
        {
            if (sfcNode != sfcProviderTopologyNode)
            {
                sfcNode.color = WHITE;
                sfcNode.dist = 0;
                sfcNode.parent = null;
            }
        }

        sfcProviderTopologyNode.color = GRAY;
        sfcProviderTopologyNode.dist = 0;
        sfcProviderTopologyNode.parent = null;

        Queue<SfcProviderTopologyNode> queue = new LinkedList<SfcProviderTopologyNode>();
        queue.offer(sfcProviderTopologyNode);

        while (!queue.isEmpty())
        {
            SfcProviderTopologyNode qSfcNode = queue.poll();
            for (SfcProviderTopologyNode sfcNode : sfcProviderTopologyEdges.get(qSfcNode))
            {
                if (sfcNode.color == WHITE)
                {
                    sfcNode.color = GRAY;
                    sfcNode.dist = qSfcNode.dist + 1;
                    queue.offer(sfcNode);
                    sfcNode.parent = qSfcNode;
                }
            }
            qSfcNode.color = BLACK;

        }
    }

    public static List<SfcProviderTopologyNode> getPath(SfcProviderTopologyNode startSfcNode, SfcProviderTopologyNode endSfcNode)
    {
        if ((sfcProviderTopologyEdges.get(startSfcNode) == null) || (sfcProviderTopologyEdges.get(endSfcNode) == null))
        {
            LOG.error(" startSfcNode or/and endSfcNode is not in the graph!");
            return null;
        }

        List<SfcProviderTopologyNode> sfcProviderTopologyNodePath = new ArrayList<SfcProviderTopologyNode>();
        if (startSfcNode.name.equals(endSfcNode.name))
        {
            sfcProviderTopologyNodePath.add(startSfcNode);
            return sfcProviderTopologyNodePath;
        }

        breadthFirstSearch(startSfcNode);
        SfcProviderTopologyNode sfcProviderTopologyNode = endSfcNode;
        while (!startSfcNode.name.equals(sfcProviderTopologyNode.name))
        {
            sfcProviderTopologyNodePath.add(sfcProviderTopologyNode);
            sfcProviderTopologyNode = sfcProviderTopologyNode.parent;
        }
        sfcProviderTopologyNodePath.add(startSfcNode);
        Collections.reverse(sfcProviderTopologyNodePath);

        return sfcProviderTopologyNodePath;
    }

    public static void printPath(List<SfcProviderTopologyNode> sfcProviderTopologyNodePath)
    {
         LOG.debug(" The sfcProviderTopologyNode Path : ");
         for (SfcProviderTopologyNode sfcProviderTopologyNode : sfcProviderTopologyNodePath)
         {
             sfcProviderTopologyNode.show();
         }
         LOG.debug(" The Path end ");
    }
}
