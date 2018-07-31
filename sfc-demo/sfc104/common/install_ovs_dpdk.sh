#!/bin/bash

if [ -x /usr/lib/openvswitch-switch-dpdk/ovs-vswitchd -a -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   exit 0
fi

# TODO execute install_ovs.sh in this same dir first

rm -rf /home/vagrant/ovs
rm -rf /home/vagrant/ovs_nsh_patches
rm -rf /home/vagrant/dpdk-2.2.0
rm -f /home/vagrant/dpdk-2.2.0.tar.gz
rm -rf /home/vagrant/dpdk-16.07
rm -f /home/vagrant/dpdk-16.07.tar.xz

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

apt-get update
apt-get install -y autoconf libtool git dh-autoreconf dh-systemd software-properties-common python-software-properties \
        libssl-dev openssl build-essential fakeroot linux-image-extra-$(uname -r) graphviz python-all python-qt4 \
        python-twisted-conch dkms git upstart

# OVS installation
git clone https://github.com/openvswitch/ovs.git
cd ovs
sudo DEB_BUILD_OPTIONS='parallel=8 nocheck' fakeroot debian/rules binary
cd ..
sudo dpkg -i ./libopenvswitch_*.deb ./openvswitch-datapath-dkms* ./openvswitch-common* ./openvswitch-switch* ./python-openvswitch*
mkdir -p /vagrant/ovs_debs
cp ./libopenvswitch_*.deb ./openvswitch-common*.deb ./openvswitch-switch*.deb /vagrant/ovs_debs/

#Install OVS-DPDK
wget http://fast.dpdk.org/rel/dpdk-16.07.tar.xz
tar xf dpdk-16.07.tar.xz
export DPDK_DIR=$(pwd)/dpdk-16.07
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
