#!/bin/bash

source /vagrant/env.sh
nsp=$(/vagrant/common/get_rsp_info.py RSP1)
rnsp=$(/vagrant/common/get_rsp_info.py RSP1-Reverse)
host=$(hostname)
if [ "${host}"  == "${CLASSIFIER1_NAME}" ] ; then
    IPV4_DST=0x$(printf '%02X' $(echo ${SFF1_VPP_IP//./ }))
    NSP=${nsp}
    RNSP=${rnsp}
    SRC_MAC=00:00:22:22:22:22
    DST_MAC=00:00:11:11:11:11
elif [ "${host}"  == "${CLASSIFIER2_NAME}" ] ; then
    IPV4_DST=0x$(printf '%02X' $(echo ${SFF2_VPP_IP//./ }))
    NSP=${rnsp}
    RNSP=${nsp}
    SRC_MAC=00:00:11:11:11:11
    DST_MAC=00:00:22:22:22:22
fi

cat << EOF > /home/vagrant/vpp-classifier-flows.txt
table=0,in_port=1 actions=NORMAL
table=0,in_port=LOCAL actions=output:1
table=0,icmp,in_port=3,nw_src=192.168.2.0/24,nw_dst=192.168.2.0/24 actions=push_nsh,load:0x1->NXM_NX_NSH_MDTYPE[],load:0x3->NXM_NX_NSH_NP[],load:${NSP}->NXM_NX_NSP[0..23],load:0xff->NXM_NX_NSI[],load:0x1->NXM_NX_NSH_C1[],load:0x9->NXM_NX_NSH_C2[],load:0x3->NXM_NX_NSH_C3[],load:0x4->NXM_NX_NSH_C4[],load:0x4->NXM_NX_TUN_GPE_NP[],load:0->NXM_NX_TUN_ID[0..31],load:${IPV4_DST}->NXM_NX_TUN_IPV4_DST[],output:2
table=0,tcp,in_port=3,nw_src=192.168.2.0/24,nw_dst=192.168.2.0/24 actions=push_nsh,load:0x1->NXM_NX_NSH_MDTYPE[],load:0x3->NXM_NX_NSH_NP[],load:${NSP}->NXM_NX_NSP[0..23],load:0xff->NXM_NX_NSI[],load:0x1->NXM_NX_NSH_C1[],load:0x9->NXM_NX_NSH_C2[],load:0x3->NXM_NX_NSH_C3[],load:0x4->NXM_NX_NSH_C4[],load:0x4->NXM_NX_TUN_GPE_NP[],load:0->NXM_NX_TUN_ID[0..31],load:${IPV4_DST}->NXM_NX_TUN_IPV4_DST[],output:2
table=0,nsp=${RNSP},nsi=253 actions=pop_nsh,set_field:${SRC_MAC}->eth_src,set_field:${DST_MAC}->eth_dst,output:3
EOF

ovs-ofctl -Oopenflow13 del-flows br-sfc
ovs-ofctl -Oopenflow13 add-flows br-sfc /home/vagrant/vpp-classifier-flows.txt

