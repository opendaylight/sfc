FROM ubuntu:15.10

MAINTAINER ericdia

RUN DEBIAN_FRONTEND=noninteractive && apt-get -qq update && apt-get -qq install -y git libtool m4 autoconf automake make \
    libssl-dev libcap-ng-dev python3 python3-pip python-six vlan iptables wget \
    net-tools init-system-helpers kmod uuid-runtime
COPY ovs-debs /tmp
RUN dpkg -i /tmp/openvswitch-common* /tmp/openvswitch-switch*
COPY ./start.sh /app/
WORKDIR /

ENTRYPOINT [ "/app/start.sh" ]
