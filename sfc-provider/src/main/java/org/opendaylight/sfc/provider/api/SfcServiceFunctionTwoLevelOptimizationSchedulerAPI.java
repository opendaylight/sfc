/*
 * Copyright (c) 2015 Guangzhou Research Institute of China Telecom. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class implements a round robin SF scheduling mode.
 * <p/>
 *
 * @author Peng Li (chinatelecom.sdn.group@gmail.com)
 * @version 0.1
 *          <p/>
 * @since 2015-03-23
 */
public class SfcServiceFunctionTwoLevelOptimizationSchedulerAPI extends SfcServiceFunctionSchedulerAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SfcServiceFunctionTwoLevelOptimizationSchedulerAPI.class);
    // each service node can open vm MAX numbers
    static int MAXVMS = 5;
    SfcServiceFunctionTwoLevelOptimizationSchedulerAPI() {
        super.setSfcServiceFunctionSchedulerType(org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.TwoLevelOptimization.class);
    }

    /*
     * Check resource information if resource is not enough run the random
     * algorithm && return 0
     */
    public int checkResource(List<SfcServiceFunction> sfcServiceFunctionList) {
        int result = 0;
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            LOG.info("ServiceFunction name: {}", sfcServiceFunction.getName());
            ServiceFunctionType serviceFunctionType;
            // find all service functions is this type
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                Set<String> serviceNodeNames = new HashSet<String>();
                int createNewServiceFunctionSign = 0;
                for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                    ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(sftServiceFunctionName.getName());
                    serviceNodeNames.add(serviceFunction.getSfDataPlaneLocator().get(0).getName());
                    if (serviceFunction.getCPUUtilization() < 99 && serviceFunction.getMemoryUtilization() < 99) {
                        // if one service function's the cpu utilization and the
                        // memory utilization are all below 99 percent
                        createNewServiceFunctionSign++;
                    }
                }
                // convert the set of serviceNodeNames to the list
                List<String> serviceNodeNamesStrings = new ArrayList<String>();
                serviceNodeNamesStrings.addAll(serviceNodeNames);
                // serviceNodeNames sort
                Collections.sort(serviceNodeNamesStrings);
                if (createNewServiceFunctionSign == 0) {
                    for (String serviceNodeName : serviceNodeNamesStrings) {
                        // if the service node already has MAXVMS service
                        // functions goto next one
                        if (SfcProviderServiceNodeAPI.readServiceNode(serviceNodeName).getServiceFunction().size() < MAXVMS) {
                            // create the new service function at this service
                            // node
                            createNewServiceFunction(serviceNodeName, sfcServiceFunction.getType());
                            createNewServiceFunctionSign++;
                            result = 1;
                            break;
                        }
                    }
                    // all service node is full find the service node beside the
                    // serviceNodeNamesStrings
                    if (createNewServiceFunctionSign == 0) {
                        for (String serviceNodeName : serviceNodeNamesStrings) {
                            if (serviceNodeName.split("-")[1].equals("1")) {
                                // the first node check the second node
                                if (SfcProviderServiceNodeAPI.readServiceNode(serviceNodeName.split("-")[0] + "2").getServiceFunction().size() < MAXVMS) {
                                    // create the service function at the second
                                    // service node
                                    createNewServiceFunction(serviceNodeName, sfcServiceFunction.getType());
                                    result = 1;
                                    break;
                                }
                            } else {
                                int serviceNodeNums = SfcProviderServiceNodeAPI.readAllServiceNodes().size();
                                if (serviceNodeName.split("-")[1].equals(String.valueOf(serviceNodeNums))) {
                                    // the last node check the second last node
                                    if (SfcProviderServiceNodeAPI.readServiceNode(serviceNodeName.split("-")[0] + (serviceNodeNums - 1)).getServiceFunction().size() < MAXVMS) {
                                        // create the service function at the
                                        // second last service node
                                        createNewServiceFunction(serviceNodeName.split("-")[0] + (serviceNodeNums - 1), sfcServiceFunction.getType());
                                        result = 1;
                                        break;
                                    }
                                } else {
                                    // the service node is in the middle
                                    int leftServiceNodeFunctions = SfcProviderServiceNodeAPI.readServiceNode(serviceNodeName.split("-")[0] + (Integer.valueOf(serviceNodeName.split("-")[1]) - 1)).getServiceFunction().size();
                                    int rightServiceNodeFunctions = SfcProviderServiceNodeAPI.readServiceNode(serviceNodeName.split("-")[0] + (Integer.valueOf(serviceNodeName.split("-")[1]) + 1)).getServiceFunction().size();
                                    if (leftServiceNodeFunctions > MAXVMS && rightServiceNodeFunctions > MAXVMS) {
                                        break;
                                    } else {
                                        if (leftServiceNodeFunctions >= rightServiceNodeFunctions) {
                                            // create the new service function
                                            // at the left service node
                                            createNewServiceFunction(serviceNodeName.split("-")[0] + (Integer.valueOf(serviceNodeName.split("-")[1]) - 1), sfcServiceFunction.getType());
                                            result = 1;
                                            break;
                                        } else {
                                            // create the new service function
                                            // at the right service node
                                            createNewServiceFunction(serviceNodeName.split("-")[0] + (Integer.valueOf(serviceNodeName.split("-")[1]) + 1), sfcServiceFunction.getType());
                                            result = 1;
                                            break;

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // default have enough resource
        return result;
    }
    /**
     * This method creates a service function .
     * <p>
     *
     * @param serviceFunctionName
     *            SF name
     * @param serviceNodeName
     *            SN name
     * @param serviceFunctionIPString
     *            SF IP
     * @param serviceFunctionTypeIdentity
     *            SF type
     * @return 1 created the new service function on the special service node
     *         create failed
     */
    public String createNewServiceFunction(String serviceNodeName, Class<? extends ServiceFunctionTypeIdentity> serviceFunctionTypeIdentity) {
        ServiceFunctionType serviceFunctionType;
        // find all service functions is this type
        serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(serviceFunctionTypeIdentity);
        String serviceFunctionName = serviceFunctionType.getType().getSimpleName().toUpperCase() + "-" + (serviceFunctionType.getSftServiceFunctionName().size() + 1);
        serviceFunctionType.getSftServiceFunctionName().size();
        try {
            // create a new service function,add a service function to datastore
            // create the service function ip address
            String serviceFunctionIPString = "10.1.4.2";
            switch (serviceFunctionName.split("-")[0]) {
                case "DPI" :
                    serviceFunctionIPString = "10.1.1.2";
                    break;
                case "FW" :
                    serviceFunctionIPString = "10.1.2.2";
                    break;
                case "NAPT44" :
                    serviceFunctionIPString = "10.1.3.2";
                    break;
                default :
                    break;
            }
            serviceFunctionIPString = serviceFunctionIPString + serviceFunctionName.split("-")[1];
            // mock create new service function
            ServiceFunctionBuilder serviceFunctionBuilder = new ServiceFunctionBuilder();
            serviceFunctionBuilder.setCPUUtilization((long) 10);
            serviceFunctionBuilder.setMemoryUtilization((long) 10);
            serviceFunctionBuilder.setIpMgmtAddress(new IpAddress(new Ipv4Address(serviceFunctionIPString)));

            serviceFunctionBuilder.setName(serviceFunctionName);
            serviceFunctionBuilder.setNshAware(true);
            Uri uri = new Uri("http://localhost:5000");
            serviceFunctionBuilder.setRestUri(uri);
            serviceFunctionBuilder.setType(serviceFunctionTypeIdentity);

            SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder = new SfDataPlaneLocatorBuilder();
            sfDataPlaneLocatorBuilder.setName(serviceNodeName);
            sfDataPlaneLocatorBuilder.setServiceFunctionForwarder(serviceNodeName);
            sfDataPlaneLocatorBuilder.setLocatorType(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(serviceNodeName).getSffDataPlaneLocator().get(0).getDataPlaneLocator().getLocatorType());
            List<SfDataPlaneLocator> sfDataPlaneLocators = new ArrayList<SfDataPlaneLocator>();
            sfDataPlaneLocators.add(sfDataPlaneLocatorBuilder.build());
            serviceFunctionBuilder.setSfDataPlaneLocator(sfDataPlaneLocators);
            // add the new service function to the datastore
            SfcProviderServiceFunctionAPI.putServiceFunction(serviceFunctionBuilder.build());

            // update the new service function to the serviceFunctionForwarder
            ServiceFunctionForwarder serviceFunctionForwarder = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(serviceNodeName);
            ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder(serviceFunctionForwarder);

            List<ServiceFunctionDictionary> serviceFunctionDictionary = serviceFunctionForwarder.getServiceFunctionDictionary();
            ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder = new ServiceFunctionDictionaryBuilder();
            SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder = new SffSfDataPlaneLocatorBuilder();
            IpBuilder ipBuilder = new IpBuilder();
            ipBuilder.setIp(new IpAddress(new Ipv4Address(serviceFunctionIPString)));
            ipBuilder.setPort(new PortNumber(5000));
            sffSfDataPlaneLocatorBuilder.setLocatorType((LocatorType) ipBuilder.build());
            serviceFunctionDictionaryBuilder.setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build());
            serviceFunctionDictionaryBuilder.setName(serviceFunctionName);
            serviceFunctionDictionaryBuilder.setType(serviceFunctionTypeIdentity);
            serviceFunctionDictionary.add(serviceFunctionDictionaryBuilder.build());
            serviceFunctionForwarderBuilder.setServiceFunctionDictionary(serviceFunctionDictionary);

            SfcProviderServiceForwarderAPI.putServiceFunctionForwarderExecutor(serviceFunctionForwarderBuilder.build());

            // update the new service function to the serviceNode
            ServiceNodeBuilder serviceNodeBuilder = new ServiceNodeBuilder(SfcProviderServiceNodeAPI.readServiceNode(serviceNodeName));
            List<String> serviceFunctionsStrings = serviceNodeBuilder.getServiceFunction();
            serviceFunctionsStrings.add(serviceFunctionName);
            serviceNodeBuilder.setServiceFunction(serviceFunctionsStrings);
            SfcProviderServiceNodeAPI.putServiceNodeExecutor(serviceNodeBuilder.build());
            return serviceFunctionName;
        } catch (Exception e) {
            LOG.info("create new service function faild: {}", e.getMessage());
            return null;
        }
    }
    /**
     * This method find all combination path of service functions .
     * <p>
     *
     * @param inputLists
     *            all type service functions lists
     * @param inputList
     *            one type service functions list
     * @index point the type of the service function
     * @param resultList
     *            single combination of the service functions
     * @param resultLists
     *            all the combination of the service functions
     */
    public static void combinationsingal(List<List<String>> inputLists, List<String> inputList, int index, List<String> resultList, List<List<String>> resultLists) {
        for (String input : inputList) {
            resultList.add(input);
            if (index != inputLists.size() - 1) {
                combinationsingal(inputLists, inputLists.get(index + 1), index + 1, resultList, resultLists);
            } else {
                List<String> resulttmp = new ArrayList<String>(resultList);
                resultLists.add(resulttmp);
            }
            resultList.remove(index);
        }
    }
    /**
     * This method find all combination path of service functions .
     * <p>
     *
     * @param sfcServiceFunctionList
     *            service functions list
     *
     * @return List of the String
     */
    public List<List<String>> serchPath(List<SfcServiceFunction> sfcServiceFunctionList) {
        //
        List<List<String>> serviceFunctionNameCombinations = new ArrayList<List<String>>();
        List<List<String>> serviceFunctionNames = new ArrayList<List<String>>();
        for (SfcServiceFunction sfcServiceFunction : sfcServiceFunctionList) {
            ServiceFunctionType serviceFunctionType;
            serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionTypeExecutor(sfcServiceFunction.getType());
            List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
            List<String> sfcServiceFunctionNameStrings = new ArrayList<String>();
            for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                sfcServiceFunctionNameStrings.add(sftServiceFunctionName.getName());
            }
            Collections.sort(sfcServiceFunctionNameStrings);
            serviceFunctionNames.add(sfcServiceFunctionNameStrings);
        }
        List<String> resultList = new ArrayList<String>();
        combinationsingal(serviceFunctionNames, serviceFunctionNames.get(0), 0, resultList, serviceFunctionNameCombinations);
        return serviceFunctionNameCombinations;
    }
    /**
     * This method find all combination path of service functions .
     * <p>
     *
     * @param possiblePath
     *            service functions possible path list
     *
     * @return List of the String
     */
    public static List<List<String>> pathLenJudge(List<List<String>> possiblePath, List<Integer> lenSort) {
        int serverLink[] = new int[possiblePath.size()];
        int lenPath[] = new int[possiblePath.size()];
        for (int i = 0; i < possiblePath.size(); i++) {
            for (int j = 0; j < possiblePath.get(0).size(); j++) {
                if (j == 0) {
                    serverLink[i] = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePath.get(i).get(j)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                } else {
                    serverLink[i] = serverLink[i]
                            + Math.abs(Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePath.get(i).get(j)).getSfDataPlaneLocator().get(0).getName().split("-")[1])
                                    - Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePath.get(i).get(j - 1)).getSfDataPlaneLocator().get(0).getName().split("-")[1]));
                }
            }
            serverLink[i] = serverLink[i] + (SfcProviderServiceNodeAPI.readAllServiceNodes().size() - Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePath.get(i).get(possiblePath.get(i).size() - 1)).getSfDataPlaneLocator().get(0).getName().split("-")[1]) + 1);
            int vmLink = possiblePath.get(0).size() * 2;
            lenPath[i] = serverLink[i] + vmLink;
        }
        int allPath = SfcProviderServiceNodeAPI.readAllServiceNodes().size() + 1 + possiblePath.get(0).size() * 2;
        List<List<String>> Path = new ArrayList<List<String>>();
        for (int i = 0; i < possiblePath.size(); i++) {
            if (lenPath[i] == allPath) {
                Path.add(possiblePath.get(i));
            }
        }
        if (Path.isEmpty()) {
            Map<List<String>, Integer> possiblePathLengthMap = new HashMap<List<String>, Integer>();
            for (int i = 0; i < possiblePath.size(); i++) {
                possiblePathLengthMap.put(possiblePath.get(i), lenPath[i]);
            }
            List<Map.Entry<List<String>, Integer>> possiblePathLengthMapList = new ArrayList<Map.Entry<List<String>, Integer>>(possiblePathLengthMap.entrySet());
            Collections.sort(possiblePathLengthMapList, new Comparator<Map.Entry<List<String>, Integer>>() {
                public int compare(Entry<List<String>, Integer> o1, Entry<List<String>, Integer> o2) {
                    return (o1.getValue() - o2.getValue());
                }
            });
            for (Entry<List<String>, Integer> entry : possiblePathLengthMapList) {
                Path.add(entry.getKey());
                lenSort.add(entry.getValue());
            }
            return Path;
        } else {
            return Path;
        }
    }
    /**
     * This method find each path of paths cost base on CPUUtilization and
     * MemoryUtilization .The order sort by CPUUtilization and MemoryUtilization
     * <p>
     *
     * @param Paths
     *            all the possible path
     *
     * @return List of the String sort by CPUUtilization and MemoryUtilization
     */
    public static List<List<String>> findBottle(List<List<String>> Paths) {
        Map<List<String>, Double> pathsWeightMap = new HashMap<List<String>, Double>();
        List<List<String>> resultLists = new ArrayList<List<String>>();
        for (int i = 0; i < Paths.size(); i++) {
            Double serverBottle = (double) 100;
            for (int j = 0; j < Paths.get(i).size(); j++) {
                long vmCPU = 100 - SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(Paths.get(i).get(j)).getCPUUtilization();
                long vmMem = 100 - SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(Paths.get(i).get(j)).getMemoryUtilization();
                double vmRes = 0.5 * (vmCPU + vmMem);
                if (vmRes < serverBottle) {
                    serverBottle = vmRes;
                }
            }
            pathsWeightMap.put(Paths.get(i), serverBottle);
        }
        List<Map.Entry<List<String>, Double>> pathsWeightMapList = new ArrayList<Map.Entry<List<String>, Double>>(pathsWeightMap.entrySet());
        Collections.sort(pathsWeightMapList, new Comparator<Map.Entry<List<String>, Double>>() {
            public int compare(Entry<List<String>, Double> o1, Entry<List<String>, Double> o2) {
                return (int) (o2.getValue() - o1.getValue());
            }
        });
        for (Entry<List<String>, Double> entry : pathsWeightMapList) {
            resultLists.add(entry.getKey());
        }
        return resultLists;
    }
    /**
     * This method find the repeat index in the lenSort array.
     * <p>
     *
     * @param lenSort
     *            the possible index array
     *
     * @return repeat index in the lenSort array.
     */
    public static List<int[]> localSort(int[] lenSort) {
        List<int[]> ret = new ArrayList<int[]>();
        int[][] localtmp = new int[lenSort.length][2];
        int valuetmp = 0;
        int sign = 0;
        for (int i = 0; i < lenSort.length; i++) {
            if (i == 0) {
                valuetmp = lenSort[i];
                localtmp[sign][0] = i;
            } else {
                if (valuetmp == lenSort[i]) {
                    localtmp[sign][1] = i;
                } else {
                    valuetmp = lenSort[i];
                    sign++;
                    localtmp[sign][0] = i;
                }
            }
        }
        for (int i = 0; i < lenSort.length; i++) {
            if (localtmp[i][1] != 0) {
                ret.add(localtmp[i]);
            }
        }
        return ret;
    }
    /**
     * This method find the remaining service function numbers between the
     * startServiceNodeIndex and the endServiceNodeIndex.
     * <p>
     *
     * @param startServiceNodeIndex
     *            the start index of service node
     * @param endServiceNodeIndex
     *            the end index of service node
     *
     * @return remainingServiceFunctionNumbers remaining service function
     *         numbers.
     */
    public static int getRemainServiceFunctionNumbers(int startServiceNodeIndex, int endServiceNodeIndex) {
        int remainingServiceFunctionNumbers = 0;
        String serviceNodeNamePrefix = "Server-";
        for (int i = startServiceNodeIndex; i <= endServiceNodeIndex; i++) {
            remainingServiceFunctionNumbers = remainingServiceFunctionNumbers + (MAXVMS - SfcProviderServiceNodeAPI.readServiceNode(serviceNodeNamePrefix + i).getServiceFunction().size());
        }
        return remainingServiceFunctionNumbers;
    }
    /**
     * This method find the least load service node between the
     * startServiceNodeIndex and the endServiceNodeIndex.
     * <p>
     *
     * @param startServiceNodeIndex
     *            the start index of service node
     * @param endServiceNodeIndex
     *            the end index of service node
     *
     * @return LeastLoadServiceNodeName Least Load service node name.
     */
    public static String getLeastLoadServiceNodeName(int startServiceNodeIndex, int endServiceNodeIndex) {
        String LeastLoadServiceNodeName = null;
        String serviceNodeNamePrefix = "Server-";
        for (int i = startServiceNodeIndex; i <= endServiceNodeIndex; i++) {
            if (i == startServiceNodeIndex) {
                LeastLoadServiceNodeName = serviceNodeNamePrefix + i;
                continue;
            }
            if (SfcProviderServiceNodeAPI.readServiceNode(serviceNodeNamePrefix + (i - 1)).getServiceFunction().size() < SfcProviderServiceNodeAPI.readServiceNode(serviceNodeNamePrefix + i).getServiceFunction().size()) {
                LeastLoadServiceNodeName = serviceNodeNamePrefix + i;
            }
        }
        return LeastLoadServiceNodeName;
    }
    /**
     * This method rectify the backtracked path to no backtracked path
     * <p>
     *
     * @param possiblePaths
     *            all possible paths is backtracked
     *
     * @return nonBacktrackedPath the first rectified path.
     */
    List<String> checkLoop(List<List<String>> possiblePaths) {
        int possiblePathNumber = possiblePaths.size();
        int servicePathLength = possiblePaths.get(0).size();
        int serviceNodeNumber = SfcProviderServiceNodeAPI.readAllServiceNodes().size();
        for (int i = 0; i < possiblePathNumber; i++) {
            List<Integer> pathType = new ArrayList<Integer>();
            List<Integer> compareResult = new ArrayList<Integer>();
            List<Integer> serviceFunctionIndexList = new ArrayList<Integer>();
            if (servicePathLength == 2) {
                int fristServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                int lastServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                if (fristServiceNodeIndex > lastServiceNodeIndex) {
                    if (getRemainServiceFunctionNumbers(1, lastServiceNodeIndex) >= 1) {
                        String leastLoadServiceNodeName = getLeastLoadServiceNodeName(1, lastServiceNodeIndex);
                        String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getType());
                        possiblePaths.get(i).remove(0);
                        possiblePaths.get(i).add(0, newServiceFunctionName);
                        return possiblePaths.get(i);
                    }
                    if (getRemainServiceFunctionNumbers(fristServiceNodeIndex, serviceNodeNumber) >= 1) {
                        String leastLoadServiceNodeName = getLeastLoadServiceNodeName(fristServiceNodeIndex, serviceNodeNumber);
                        String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getType());
                        possiblePaths.get(i).remove(1);
                        possiblePaths.get(i).add(1, newServiceFunctionName);
                        return possiblePaths.get(i);
                    } else {
                        return null;
                    }
                }
            }
            if (servicePathLength > 2) {
                // judge the type of the path
                for (int j = 0; j < servicePathLength - 2; j++) {
                    int fristServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                    int middleServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                    int lastServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(2)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                    if (fristServiceNodeIndex <= middleServiceNodeIndex && middleServiceNodeIndex <= lastServiceNodeIndex) {
                        continue;
                    }
                    if (fristServiceNodeIndex > middleServiceNodeIndex && middleServiceNodeIndex <= lastServiceNodeIndex) {
                        pathType.add(1);
                        serviceFunctionIndexList.add(j);
                        if (fristServiceNodeIndex > lastServiceNodeIndex) {
                            compareResult.add(1);
                        } else {
                            compareResult.add(2);
                        }
                    }
                    if (fristServiceNodeIndex <= middleServiceNodeIndex && middleServiceNodeIndex > lastServiceNodeIndex) {
                        pathType.add(2);
                        serviceFunctionIndexList.add(j);
                        if (fristServiceNodeIndex > lastServiceNodeIndex) {
                            compareResult.add(1);
                        } else {
                            compareResult.add(2);
                        }
                    }
                    if (fristServiceNodeIndex > middleServiceNodeIndex && middleServiceNodeIndex > lastServiceNodeIndex) {
                        pathType.add(3);
                        serviceFunctionIndexList.add(j);
                        compareResult.add(-1);
                    }
                }
                if (pathType.size() == 1) {
                    int fristServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                    int middleServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                    int lastServiceNodeIndex = Integer.valueOf(SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(2)).getSfDataPlaneLocator().get(0).getName().split("-")[1]);
                    if (fristServiceNodeIndex <= middleServiceNodeIndex && middleServiceNodeIndex > lastServiceNodeIndex) {
                        if (fristServiceNodeIndex > lastServiceNodeIndex) {
                            if (getRemainServiceFunctionNumbers(middleServiceNodeIndex, serviceNodeNumber) >= 1) {
                                if (servicePathLength == 3) {
                                    String leastLoadServiceNodeName = getLeastLoadServiceNodeName(middleServiceNodeIndex, serviceNodeNumber);
                                    String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(2)).getType());
                                    possiblePaths.get(i).remove(2);
                                    possiblePaths.get(i).add(2, newServiceFunctionName);
                                    return possiblePaths.get(i);
                                }
                            }
                        } else {
                            if (getRemainServiceFunctionNumbers(fristServiceNodeIndex, lastServiceNodeIndex) >= 1) {
                                if (servicePathLength == 3) {
                                    String leastLoadServiceNodeName = getLeastLoadServiceNodeName(fristServiceNodeIndex, lastServiceNodeIndex);
                                    String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getType());
                                    possiblePaths.get(i).remove(1);
                                    possiblePaths.get(i).add(1, newServiceFunctionName);
                                    return possiblePaths.get(i);
                                }
                            }
                            if (getRemainServiceFunctionNumbers(middleServiceNodeIndex, serviceNodeNumber) >= 1) {
                                String leastLoadServiceNodeName = getLeastLoadServiceNodeName(middleServiceNodeIndex, serviceNodeNumber);
                                String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(2)).getType());
                                possiblePaths.get(i).remove(2);
                                possiblePaths.get(i).add(2, newServiceFunctionName);
                                return possiblePaths.get(i);
                            }
                        }
                    }
                    if (fristServiceNodeIndex > middleServiceNodeIndex && middleServiceNodeIndex <= lastServiceNodeIndex) {
                        if (fristServiceNodeIndex > lastServiceNodeIndex) {
                            if (getRemainServiceFunctionNumbers(1, middleServiceNodeIndex) >= 1) {
                                String leastLoadServiceNodeName = getLeastLoadServiceNodeName(1, middleServiceNodeIndex);
                                String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getType());
                                possiblePaths.get(i).remove(0);
                                possiblePaths.get(i).add(0, newServiceFunctionName);
                                return possiblePaths.get(i);
                            }
                        } else {
                            if (getRemainServiceFunctionNumbers(1, middleServiceNodeIndex) >= 1) {
                                String leastLoadServiceNodeName = getLeastLoadServiceNodeName(1, middleServiceNodeIndex);
                                String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getType());
                                possiblePaths.get(i).remove(0);
                                possiblePaths.get(i).add(0, newServiceFunctionName);
                                return possiblePaths.get(i);
                            }
                            if (getRemainServiceFunctionNumbers(fristServiceNodeIndex, lastServiceNodeIndex) >= 1) {
                                String leastLoadServiceNodeName = getLeastLoadServiceNodeName(fristServiceNodeIndex, lastServiceNodeIndex);
                                String newServiceFunctionName = createNewServiceFunction(leastLoadServiceNodeName, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getType());
                                possiblePaths.get(i).remove(1);
                                possiblePaths.get(i).add(1, newServiceFunctionName);
                                return possiblePaths.get(i);
                            }
                        }
                    }
                    if (fristServiceNodeIndex > middleServiceNodeIndex && middleServiceNodeIndex > lastServiceNodeIndex) {
                        if (getRemainServiceFunctionNumbers(1, lastServiceNodeIndex) >= 2) {
                            String leastLoadServiceNodeNameFrist = getLeastLoadServiceNodeName(1, lastServiceNodeIndex);
                            String newServiceFunctionNameFrist = createNewServiceFunction(leastLoadServiceNodeNameFrist, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(0)).getType());
                            possiblePaths.get(i).remove(0);
                            possiblePaths.get(i).add(0, newServiceFunctionNameFrist);
                            // get the new service function belong which
                            // service node
                            String tmpServiceNodeName = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(newServiceFunctionNameFrist).getSfDataPlaneLocator().get(0).getName();
                            String leastLoadServiceNodeNameSecond = getLeastLoadServiceNodeName(Integer.valueOf(tmpServiceNodeName.split("-")[1]), lastServiceNodeIndex);
                            String newServiceFunctionNameSecond = createNewServiceFunction(leastLoadServiceNodeNameSecond, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getType());
                            possiblePaths.get(i).remove(1);
                            possiblePaths.get(i).add(1, newServiceFunctionNameSecond);
                            return possiblePaths.get(i);
                        }
                        if (getRemainServiceFunctionNumbers(fristServiceNodeIndex, serviceNodeNumber) >= 2) {
                            String leastLoadServiceNodeNameFrist = getLeastLoadServiceNodeName(fristServiceNodeIndex, serviceNodeNumber);
                            String newServiceFunctionNameFrist = createNewServiceFunction(leastLoadServiceNodeNameFrist, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(1)).getType());
                            possiblePaths.get(i).remove(1);
                            possiblePaths.get(i).add(1, newServiceFunctionNameFrist);
                            // get the new service function belong which
                            // service node
                            String tmpServiceNodeName = SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(newServiceFunctionNameFrist).getSfDataPlaneLocator().get(0).getName();
                            String leastLoadServiceNodeNameSecond = getLeastLoadServiceNodeName(Integer.valueOf(tmpServiceNodeName.split("-")[1]), serviceNodeNumber);
                            String newServiceFunctionNameSecond = createNewServiceFunction(leastLoadServiceNodeNameSecond, SfcProviderServiceFunctionAPI.readServiceFunctionExecutor(possiblePaths.get(i).get(2)).getType());
                            possiblePaths.get(i).remove(2);
                            possiblePaths.get(i).add(2, newServiceFunctionNameSecond);
                            return possiblePaths.get(i);
                        }
                    }
                }
            }
        }
        return null;
    }
    public List<String> scheduleServiceFuntions(ServiceFunctionChain chain, int serviceIndex) {
        // List<String> sfNameList = new ArrayList<>();
        List<SfcServiceFunction> sfcServiceFunctionList = new ArrayList<>();

        sfcServiceFunctionList.addAll(chain.getSfcServiceFunction());
        /*
         * Check resource information return 0 means use random algorithm
         */
        checkResource(sfcServiceFunctionList);
        /*
         * find all the service functions combinations
         */
        List<List<String>> serviceFunctionNameCombinations = serchPath(sfcServiceFunctionList);
        /*
         * For each ServiceFunction type in the list of ServiceFunctions we
         * select a specific service function from the list of service functions
         * by type.
         */
        List<Integer> lenSort = new ArrayList<Integer>();
        List<List<String>> possiblePaths = pathLenJudge(serviceFunctionNameCombinations, lenSort);
        if (lenSort.isEmpty()) {
            // possiblePaths are not backtrack
            List<List<String>> possiblePathsSortByWeight = findBottle(possiblePaths);
            return possiblePathsSortByWeight.get(0);
        } else {
            // all possible paths are backtrack
            int[] inputPossbilePathIndex = new int[lenSort.size()];
            for (int i = 0; i < lenSort.size(); i++) {
                inputPossbilePathIndex[i] = lenSort.get(i);
            }
            List<int[]> repeatIndexList = localSort(inputPossbilePathIndex);
            for (int j = 0; j < repeatIndexList.size(); j++) {
                List<List<String>> temp = new ArrayList<List<String>>();
                for (int k = repeatIndexList.get(j)[0]; k < repeatIndexList.get(j)[1] + 1; k++) {
                    temp.add(possiblePaths.get(k));
                }
                List<List<String>> tempAfter = findBottle(temp);
                for (int k = repeatIndexList.get(j)[0], index = 0; k < repeatIndexList.get(j)[1] + 1; k++, index++) {
                    possiblePaths.remove(k);
                    possiblePaths.add(k, tempAfter.get(index));
                }
            }
            List<String> nobacktrackpath = checkLoop(possiblePaths);
            if (nobacktrackpath != null) {
                return nobacktrackpath;
            } else {
                return possiblePaths.get(0);
            }
        }
    }
}
