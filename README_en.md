#Description#
This project is branch of [service function chain](https://wiki.opendaylight.org/view/Service_Function_Chaining:Main) project belonging to OpenDaylight.  We add a self-defined greeady algorithm called Two-Level-Optimization in master code of service function chain.  The algorithm implements optimized service chain routing, with the goal of maximum throughput.  Every time when a customer arrives, the step of resource-check is wakened to judge whether there are enough resources to serve the customer.  If the resource is insufficient, a new virtual machine is created and powered on.  In daisy-chain network connection mode, the issue of path retracing might happen, which leads to the waste of link bandwidth.  Two-Level-Optimization algorithm solves the problem by introducing a mechanism to prevent path retracing.  Besides, every time when choosing a service chain path, the algorithm chooses the one with the ability to serve the most customers in order to evenly utilize the resource of virtual machines.

* Imitation of powering on a virtual machine.  In order to imitate creating and powering on a new virtual machine, we write information of new virtual machine into datastore of OpenDaylight.
* virtual machine resource includes: cpu utilization ratio and memory utilization ratio.

# source code addition and modification#
##** sfc-model/service-function.yang**## 
Algorithm needs monitor information of virtual machine, but the /sfc-provider/bootstrap/* json config files does not support the addition of service function monitor information.  Besides, there is no real virtual machine monitor program to collect relative information, so we modify the service-function.yang file to directly add CPU and memory utilization ratio into list service-function of container service-functions.  We will utilize the unified collection API to acquire virtual machine monitor information when real monitor collection program exists.

##sfc-model/service-function-scheduler-type.yang##
Add algorithm name “two-level-optimization” into schedule options.

##sfc-jsonconfig/bootstrap##
These are files contain initialization data of service node, virtual machine, service function chain and service function forwarder.  Here are some naming rules:
1.	The names of service node and service function forwarder begin with “Server-” and follow with a relative serial number.
2.	The names of service function begin with function type name and follow with a serial number.  For example, service function name of DPI is "DPI-1", "DPI-2".

##sfc-ui/resources/data/locale-en_US.json##
Add algorithm name “two-level-optimization” into the file.

##sfc-ui/resources/src/servicechain/servicechain.modal.instantiate.tpl.html##
Add algorithm name “two-level-optimization” into algorithm select box.

##sfc-provider/api/SfcProviderServiceNodeAPI.java##
Provide API to access data of service node in datastore.

##sfc-provider/api/SfcServiceFunctionTwoLevelOptimizationSchedulerAPI.java##
algorithm main function

##sfc-provider/api/SfcProviderRenderedPathAPI.java##
Add “two-level-optimization” algorithm option into path schedule function.

#source code utilization#
For compiling and running, please refer to the description in service function chain project of OpenDaylight.  When finding the path of [service function chain](https://wiki.opendaylight.org/view/Service_Function_Chaining:Main), please choose Two-Level-Optimization algorithm. 
#Corporation#

* Guangzhou Research Institute of China Telecom 

#Author#
##Algorithm design##
* Yujia Luo(chinatelecom.sdn.group@gmail.com)
* Hong Tang(chinatelecom.sdn.group@gmail.com)
##Coding##
* Peng Li (chinatelecom.sdn.group@gmail.com)