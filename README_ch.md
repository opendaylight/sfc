#简介#

  这个项目是opendaylight中[service function chain项目](https://wiki.opendaylight.org/view/Service_Function_Chaining:Main)的一个分支。在service function chain主线代码中添加自定义的TwoLevelOptimization贪婪算法。该算法实现了业务链路由优化，目标是均衡使用链路带宽资源和VM资源，使得用户吞吐量最大。每次用户到来时，算法需要进行资源检查步骤去判定资源是否充足。如果资源不足以服务用户，则需要开启新的虚机。在串行组网连接方式下，业务链路径可能会出现折返问题，从而浪费链路带宽资源。本算法引入折返纠正机制，优化了链路带宽资源使用。另外在选路时，每次选择能够接待用户数最多的一条路径，从而保证了对虚机资源的均衡使用。

*   模拟开启虚机，在opendaylight的datastore中写入新虚机信息，模拟开启新虚拟机的操作
*   虚机资源包括：cpu利用率和内存利用率

#service function chain 代码的增加与修改#
##**sfc-model/service-function.yang**##
算法需要使用虚拟机状态信息，但现有的json初始化数据的接口无法批量添加service function的状态信息，同时也没有真实的虚机监控程序采集对应数据，所以修改了service-function.yang文件。在container service-functions的list service-function中直接加入了cpu利用率及内存利用率。后期有对应的监控采集程序后，可以使用统一的采集接口获取虚拟机状态信息。

##sfc-model/service-function-scheduler-type.yang##
 在调度选项中增加新算法名称"two-level-optimization"

##sfc-jsonconfig/bootstrap##
 该文件目录为service function chain的初始化数据文件目录，分别定义了物理服务器（service node）、虚拟机（service function）、业务链（service function chain）和转发器（service function forwarder）的初始数据。在此约定一些命名规则：

1. service node和service function forwarder的名字都是以"Server-"开头后加上对应编号
2. service function的名字是在对应类型名后面加上编号如，DPI类型的service function名字为"DPI-1"，"DPI-2"

##sfc-ui/resources/data/locale-en_US.json##
在该文件中加入了算法名

##sfc-ui/resources/src/servicechain/servicechain.modal.instantiate.tpl.html##
 在选择算法的选择框中增加算法名称

##sfc-provider/api/SfcProviderServiceNodeAPI.java##
 提供对datastore中service node数据的操作接口

##sfc-provider/api/SfcServiceFunctionTwoLevelOptimizationSchedulerAPI.java##
 算法主体函数

##sfc-provider/api/SfcProviderRenderedPathAPI.java##
 在调度路径的函数中增加新算法的选项

#代码使用#
参照opendaylight中[service function chain项目](https://wiki.opendaylight.org/view/Service_Function_Chaining:Main)中的介绍编译运行，在生成业务路径时选择Two Level Optimization即可。
#公司#

* Guangzhou Research Institute of China Telecom 

#作者#
##算法设计##
* Yujia Luo(chinatelecom.sdn.group@gmail.com)
* Hong Tang(chinatelecom.sdn.group@gmail.com)
##算法实现##
* Peng Li (chinatelecom.sdn.group@gmail.com)