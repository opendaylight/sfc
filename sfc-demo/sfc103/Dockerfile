FROM ubuntu:14.04

MAINTAINER ericdia

ENV CONTROLLER=192.168.1.5
ENV BRIDGE=br-sfc

RUN apt-get update && apt-get install -y git libtool m4 autoconf automake make \
    libssl-dev libcap-ng-dev python3 python3-pip python-six vlan iptables wget
RUN git clone https://github.com/pritesh/ovs.git -b nsh-v8
WORKDIR ovs
RUN ./boot.sh && ./configure --prefix=/usr --localstatedir=/var --sysconfdir=/etc
RUN make && make install
RUN mkdir -p /etc/openvswitch && \
    ovsdb-tool create /etc/openvswitch/conf.db vswitchd/vswitch.ovsschema
RUN cd ..; rm -rf ovs*
ADD ./start.sh /app/
WORKDIR /

ENTRYPOINT [ "/app/start.sh" ]
