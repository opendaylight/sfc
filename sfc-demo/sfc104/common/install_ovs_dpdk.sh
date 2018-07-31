#!/bin/bash

if [ -x /usr/lib/openvswitch-switch-dpdk/ovs-vswitchd -a -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   echo "OVS DPDK has already been installed"
   exit 0
fi

HTTP_PROXY=$1
HTTPS_PROXY=$2

echo -n "" > /etc/apt/apt.conf
if [ "${HTTP_PROXY}" != "" ] ; then
    export http_proxy=${HTTP_PROXY}
    echo -e "Acquire::http::Proxy \"${HTTP_PROXY}\";" >> /etc/apt/apt.conf
fi
if [ "${HTTPS_PROXY}" != "" ] ; then
    export https_proxy=${HTTPS_PROXY}
    echo -e "Acquire::https::Proxy \"${HTTPS_PROXY}\";" >> /etc/apt/apt.conf
fi
echo "Acquire::http::No-Cache true;" >> /etc/apt/apt.conf
echo "Acquire::http::Pipeline-Depth 0;" >> /etc/apt/apt.conf
echo "Acquire::https::No-Cache true;" >> /etc/apt/apt.conf
echo "Acquire::https::Pipeline-Depth 0;" >> /etc/apt/apt.conf
cat /etc/apt/apt.conf

# Install OVS
/vagrant/common/install_ovs.sh

# OVS DPDK specific packages
apt-get update
apt-get install -y libnuma-dev

# Install OVS-DPDK
wget http://fast.dpdk.org/rel/dpdk-17.11.3.tar.xz
tar xf dpdk-17.11.3.tar.xz
export DPDK_DIR=$(pwd)/dpdk-stable-17.11.3
export DPDK_TARGET=x86_64-native-linuxapp-gcc
export DPDK_BUILD=$DPDK_DIR/$DPDK_TARGET
cd $DPDK_DIR
make install T=$DPDK_TARGET DESTDIR=install
cd ../ovs
make clean
./boot.sh
./configure --prefix=/ --with-dpdk=$DPDK_BUILD
make
mkdir -p /usr/lib/openvswitch-switch-dpdk/
cp vswitchd/ovs-vswitchd /usr/lib/openvswitch-switch-dpdk/ovs-vswitchd
echo 3 > /proc/sys/vm/drop_caches
echo "vm.nr_hugepages=1024" > /etc/sysctl.d/20-hugepages.conf
sysctl --system
cat << EOF > /etc/init/hugepages.conf
start on runlevel [2345]

task

script
    mkdir -p /run/hugepages/kvm || true
    rm -f /run/hugepages/kvm/* || true
    rm -f /dev/shm/* || true
    mount -t hugetlbfs nodev /run/hugepages/kvm
end script
EOF

service openvswitch-switch restart

if [ -x /usr/lib/openvswitch-switch-dpdk/ovs-vswitchd -a -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   exit 0
fi
exit -1
