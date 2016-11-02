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

apt-get install -y linux-image-extra-$(uname -r) vpp vpp-dpdk-dkms vpp-nsh-plugin honeycomb

mkdir -p vpp-local-debs
cd vpp-local-debs
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/vpp-local-debs/vpp_16.09-1~g3958e7a_amd64.deb
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/vpp-local-debs/vpp-dpdk-dkms_16.09-1~g3958e7a_amd64.deb
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/vpp-local-debs/vpp-lib_16.09-1~g3958e7a_amd64.deb
wget https://github.com/yyang13/ovs_nsh_patches/raw/master/vpp-local-debs/vpp-plugins_16.09-1~g3958e7a_amd64.deb
dpkg -i *.deb

sed -i 's/"127.0.0.1"/"0.0.0.0"/g' /opt/honeycomb/config/honeycomb.json
sed -i 's/"false"/"true"/' /opt/honeycomb/config/vppnsh.json

if [ -x /usr/bin/vpp -a -x /opt/honeycomb/honeycomb ] ; then
   exit 0
fi
exit -1
