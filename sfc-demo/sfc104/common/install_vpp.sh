#!/bin/bash

if [ -x /usr/bin/vpp -a -x /opt/honeycomb/honeycomb ] ; then
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

apt-get install software-properties-common python-software-properties
add-apt-repository -r ppa:openjdk-r/ppa -y
add-apt-repository ppa:openjdk-r/ppa -y
rm -f /etc/apt/sources.list.d/99fd.io.list
echo "deb [trusted=yes] https://nexus.fd.io/content/repositories/fd.io.ubuntu.trusty.main/ ./" | tee -a /etc/apt/sources.list.d/99fd.io.list
apt-get update
apt-get install openjdk-8-jdk -y

#Install vpp and honeycomb
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

apt-get install -y linux-image-extra-$(uname -r)

#Remove the old installation
dpkg -r honeycomb
dpkg --purge honeycomb
dpkg -r vpp-nsh-plugin
dpkg --purge vpp-nsh-plugin
dpkg -r vpp-plugins
dpkg --purge vpp-plugins
dpkg -r vpp-dpdk-dkms
dpkg --purge vpp-dpdk-dkms
dpkg -r vpp
dpkg --purge vpp
dpkg -r vpp-lib
dpkg --purge vpp-lib

rm -rf vpp-local-debs
rm -rf FD.io.debs
mkdir -p FD.io.debs/vpp-17.01
cd FD.io.debs/vpp-17.01
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/FD.io.debs/vpp-17.01/vpp_17.01-release_amd64.deb
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/FD.io.debs/vpp-17.01/vpp-plugins_17.01-release_amd64.deb
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/FD.io.debs/vpp-17.01/vpp-lib_17.01-release_amd64.deb
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/FD.io.debs/vpp-17.01/vpp-dpdk-dkms_17.01-release_amd64.deb

dpkg -i *.deb

cd ../..
mkdir -p FD.io.debs/nsh_sfc-17.01
cd FD.io.debs/nsh_sfc-17.01
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/FD.io.debs/nsh_sfc-17.01/vpp-nsh-plugin_17.01-rc1~4-gdf80b87_amd64.deb

dpkg -i vpp-nsh-plugin_17.01-rc1~4-gdf80b87_amd64.deb

cd ../..
mkdir -p FD.io.debs/hc2vpp-17.01
cd FD.io.debs/hc2vpp-17.01
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/FD.io.debs/hc2vpp-17.01/honeycomb_1.17.01-2033_all.deb

dpkg -i honeycomb_1.17.01-2033_all.deb

sed -i 's/"127.0.0.1"/"0.0.0.0"/g' /opt/honeycomb/config/honeycomb.json
rm -f /opt/honeycomb/config/vppnsh.json
cat >> /opt/honeycomb/config/vppnsh.json << EOF
{
  "nsh-enabled": "true"
}
EOF
sed -i 's/"false"/"true"/' /opt/honeycomb/config/vppnsh.json

if [ -x /usr/bin/vpp -a -x /opt/honeycomb/honeycomb ] ; then
   exit 0
fi
exit -1
