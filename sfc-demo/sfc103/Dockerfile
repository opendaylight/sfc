FROM ubuntu:15.10

MAINTAINER ericdia

ENV CONTROLLER=192.168.1.5
ENV BRIDGE=br-sfc

RUN apt-get update && apt-get install -y git libtool m4 autoconf automake make \
    libssl-dev libcap-ng-dev python3 python3-pip python-six vlan iptables wget \
    net-tools init-system-helpers kmod uuid-runtime
ADD ovs-debs /tmp
RUN dpkg -i /tmp/openvswitch-common* /tmp/openvswitch-switch*
ADD ./start.sh /app/
WORKDIR /

ENTRYPOINT [ "/app/start.sh" ]
