#!/bin/bash

if [ -x /usr/lib/openvswitch-switch-dpdk/ovs-vswitchd -a -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   exit 0
fi

rm -rf /home/vagrant/ovs
rm -rf /home/vagrant/dpdk-2.2.0
rm -f /home/vagrant/dpdk-2.2.0.tar.gz

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
apt-get install -y autoconf libtool git dh-autoreconf dh-systemd software-properties-common python-software-properties libssl-dev openssl build-essential fakeroot linux-image-extra-$(uname -r) graphviz python-all python-qt4 python-twisted-conch

curl https://raw.githubusercontent.com/yyang13/ovs_nsh_patches/master/start-ovs-deb.sh | bash

#Install OVS-DPDK
wget http://dpdk.org/rel/dpdk-2.2.0.tar.gz
tar xf dpdk-2.2.0.tar.gz
export DPDK_DIR=$(pwd)/dpdk-2.2.0
cd $DPDK_DIR
sed -i 's/CONFIG_RTE_BUILD_COMBINE_LIBS=n/CONFIG_RTE_BUILD_COMBINE_LIBS=y/g' config/common_linuxapp
make install T=x86_64-native-linuxapp-gcc DESTDIR=install
export DPDK_BUILD=$DPDK_DIR/x86_64-native-linuxapp-gcc/
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
nr_hugepages=$(cat /proc/sys/vm/nr_hugepages)
if [ "$nr_hugepages" != "1024" ] ; then
    echo "---"
    echo -n "  Allocating hugepages... "
    start hugepages
    nr_hugepages=$(cat /proc/sys/vm/nr_hugepages)
    echo "  nr_hugepages = $nr_hugepages"
    echo "---"
fi

service openvswitch-switch restart

if [ -x /usr/lib/openvswitch-switch-dpdk/ovs-vswitchd -a -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   exit 0
fi
exit -1
