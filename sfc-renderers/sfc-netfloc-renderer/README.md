<div align="center">
<a align="center" href='https://www.fiware.org/'>
<img src="https://raw.githubusercontent.com/icclab/netfloc/master/docs/img/netfloc.png" title="SESAME_logo" width=400px>
</a>
</div>

# What is Netfloc?

NETwork FLOws for Clouds (Netfloc) is SDN-based SDK for datacenter network programming. It is comprised of set of tools and libraries packed as Java bundles that interoperate with the OpenDaylight controller. Netfloc exposes REST API abstractions and Java interfaces for network programmers to enable optimal integration in cloud datacenters and fully SDN-enabled end-to-end management of OpenFlow enabled switches.


## Installation and Testing

Clone and install Netfloc in the sdn-control node:

```
$git clone https://github.com/icclab/netfloc.git
$cd netfloc
$mvn clean install -o
```

For the automation of Netfloc deployment, you can use [ansible-netfloc scripts](https://github.com/T-NOVA/netfloc/tree/master/ansible_installation).
## Netfloc APIs

The Service Function Chain basic APIs are fully functional at the moment. Netfloc defines API Specification for the following network resources, for which development of Northbound APIs has been scheduled:

- Tenant filtered network graph
- All host ports
- End-to-end network path
- Flow patterns on network paths
- Chain patterns

### Service Function Chain support

To create SFC in Netfloc via HEAT, please follow the guide in the [netfloc-heat project](https://github.com/icclab/netfloc-heat).

There is information on the SFC APIs in [Netfloc APIs](http://icclab.github.io/netfloc/docs/netfloc_api_spec/netfloc.html).

The [T-NOVA sfc-demo example](https://github.com/T-NOVA/netfloc-demo) is the first complete end-to-end demo of the SFC library, in integrated NFV-cloud scenario.

A more extensive documentation can be found here:
[Netfloc](https://github.com/icclab/netfloc)

