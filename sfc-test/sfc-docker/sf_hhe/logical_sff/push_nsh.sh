#!/bin/bash

if [ $(id -u) -ne 0 ]; then
   echo "ERROR script has to be run as root"
   exit -1
fi

function print_usage {
    echo "Usage:"
    echo " $0 -o <odl_url> -c <client> -s <server> -r <rsp_name>"
    echo "Example: "
    echo " $0 -o 172.28.128.31 -c client1 -s server1 -r RSP1"
}

function get_tap_port {
    local node_name=$1
    echo $(docker exec ${node_name} ovs-ofctl -O OpenFlow13 show br-int|grep tap| cut -d'(' -f 1 | sed -e 's/^[[:space:]]*//')
}

# docker exec client1 ip addr sh | grep -A 2 ": eth1" | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)'
function get_ip {
    local node_name=$1
    local interface=$2
    echo $(docker exec ${node_name} ip addr sh | grep -A 2 ": ${interface}" | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)')
}

function push_nsh {
    inspect_cmd="docker network inspect dovs-tun"
    echo "inspect_cmd("${inspect_cmd}")"
    eval $inspect_cmd

    client_ovs_vsctl_show_cmd="docker exec "${client_node}" ovs-vsctl show"
    echo "client_ovs_vsctl_show_cmd("${client_ovs_vsctl_show_cmd}")"
    eval $client_ovs_vsctl_show_cmd

    server_ovs_vsctl_show_cmd="docker exec "${server_node}" ovs-vsctl show"
    echo "server_ovs_vsctl_show_cmd("${server_ovs_vsctl_show_cmd}")"
    eval $server_ovs_vsctl_show_cmd

    client_tap_port=$(get_tap_port ${client_node})
    server_tap_port=$(get_tap_port ${server_node})

    rsp_info=$(curl --silent -X GET -H "Content-Type: application/json" -H "Accept: application/json" --user admin:admin http://${odl_url}:8181/restconf/operational/rendered-service-path:rendered-service-paths/rendered-service-path/${rsp_name})
    path_id=$(echo ${rsp_info} | grep -o -P '(?<="path-id":)[0-9]*')
    symmetric_path_id=$(echo ${rsp_info} | grep -o -P '(?<="symmetric-path-id":)[0-9]*')
    symmetric_path_id_hex=$( printf "%x" $symmetric_path_id )
    path_id_hex=$( printf "%x" $path_id )
    first_sf=$(echo ${rsp_info} | grep -o -P '(?<="service-function-name":).*' | cut -d'"' -f 2)
    last_sf=$(echo ${rsp_info} | awk 'BEGIN { FS = "service-function-name\":" } ; {print $(NF)}' | cut -d'"' -f 2 )
    # eth1 is used, because eth0 is used for management interface
    first_sf_ip=$(get_ip ${first_sf} eth1)
    last_sf_ip=$(get_ip ${last_sf} eth1)
    client_node_ip=$(get_ip ${client_node} eth0)
    server_node_ip=$(get_ip ${server_node} eth0)

    # 'sudo ip netns exec'
    client_ip=$(ip netns exec dovs-${client_node} ip addr sh | grep -A 2 ": eth0" | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)')
    server_ip=$(ip netns exec dovs-${server_node} ip addr sh | grep -A 2 ": eth0" | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)')
    client_mac=$(ip netns exec dovs-${client_node} ip a|grep -A 1 eth0|grep link|awk '{print $2}')
    server_mac=$(ip netns exec dovs-${server_node} ip a|grep -A 1 eth0|grep link|awk '{print $2}')

    client_tun_port_cmd="docker exec "${client_node}" ovs-vsctl show | grep -E -B 2 'gpe.*remote_ip.*${first_sf_ip}|remote_ip.*${first_sf_ip}.*gpe' | grep tun| cut -d'\"' -f 2"
    client_tun_port=`eval $client_tun_port_cmd |& grep tun`
    client_tun_port_number_cmd="docker exec ${client_node} ovs-ofctl -O OpenFlow13 show br-int|grep ${client_tun_port}|cut -d'(' -f 1 | sed -e 's/^[[:space:]]*//'"
    client_tun_port_number=`eval $client_tun_port_number_cmd`
    server_tun_port_cmd="docker exec "${server_node}" ovs-vsctl show | grep -E -B 2 'gpe.*remote_ip.*${last_sf_ip}|remote_ip.*${last_sf_ip}.*gpe' | grep tun| cut -d'\"' -f 2"
    server_tun_port=`eval $server_tun_port_cmd |& grep tun`
    server_tun_port_number_cmd="docker exec ${server_node} ovs-ofctl -O OpenFlow13 show br-int|grep ${server_tun_port}|cut -d'(' -f 1 | sed -e 's/^[[:space:]]*//'"
    server_tun_port_number=`eval $server_tun_port_number_cmd`
    echo "client_tap_port("${client_tap_port}")"
    echo "server_tap_port("${server_tap_port}")"
    echo "rsp_info("${rsp_info}")"
    echo "path_id("${path_id}")(0x"${path_id_hex}")"
    echo "symmetric_path_id("${symmetric_path_id}")(0x"${symmetric_path_id_hex}")"
    echo "first_sf("${first_sf}")("${first_sf_ip}")"
    echo "last_sf("${last_sf}")("${last_sf_ip}")"
    echo "client_node("${client_node}")("${client_node_ip}")"
    echo "server_node("${server_node}")("${server_node_ip}")"
    echo "client_tun_port("${client_tun_port_cmd}")("${client_tun_port}")"
    echo "client_tun_port_number_cmd("${client_tun_port_number_cmd}")("${client_tun_port_number}")"
    echo "server_tun_port("${server_tun_port_cmd}")("${server_tun_port}")"
    echo "server_tun_port_number_cmd("${server_tun_port_number_cmd}")("${server_tun_port_number}")"
    client_cmd="docker exec ${client_node} ovs-ofctl -O OpenFlow13 add-flow br-int \"table=0,priority=2001,in_port=${client_tap_port},ip actions=push_nsh,load:0x1->NXM_NX_NSH_MDTYPE[], load:0x3->NXM_NX_NSH_NP[],load:0x${path_id_hex}->NXM_NX_NSP[0..23], load:0xff->NXM_NX_NSI[], load:0x4->NXM_NX_TUN_GPE_NP[], output:${client_tun_port_number}\""
    server_cmd="docker exec ${server_node} ovs-ofctl -O OpenFlow13 add-flow br-int \"table=0,priority=2001,in_port=${server_tap_port},ip actions=push_nsh,load:0x1->NXM_NX_NSH_MDTYPE[], load:0x3->NXM_NX_NSH_NP[],load:0x${symmetric_path_id_hex}->NXM_NX_NSP[0..23], load:0xff->NXM_NX_NSI[],load:0x4->NXM_NX_TUN_GPE_NP[],output:${server_tun_port_number}\""

    echo "client_cmd("${client_cmd}")"
    eval $client_cmd
    docker exec ${client_node} ovs-ofctl -O OpenFlow13 dump-flows br-int | grep nsh
    echo "server_cmd("${server_cmd}")"
    eval $server_cmd
    docker exec ${server_node} ovs-ofctl -O OpenFlow13 dump-flows br-int | grep nsh

    # sudo ip netns exec
    cmd_arp_server="ip netns exec dovs-${client_node} arp -s ${server_ip} ${server_mac}"
    echo "cmd_arp_server("${cmd_arp_server}")"
    eval $cmd_arp_server
    cmd_arp_client="ip netns exec dovs-${server_node} arp -s ${client_ip} ${client_mac}"
    echo "cmd_arp_client("${cmd_arp_client}")"
    eval $cmd_arp_client
}

while getopts o:c:s:r: option
do
    case "${option}"
    in
        o) odl_url=${OPTARG};;
        c) client_node=${OPTARG};;
        s) server_node=${OPTARG};;
        r) rsp_name=$OPTARG;;
        h  ) print_usage $0; exit;;
        \? ) echo "Unknown option: -${OPTARG}" >&2; exit 1;;
        :  ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
        *  ) echo "Unimplemented option: -$OPTARG" >&2; exit 1;;
    esac
done

if [ $OPTIND -ne 9 ]; then
    echo "Incorrect number of parameters";
    print_usage $0
    exit 1
fi
shift $((OPTIND-1))
#echo "$# non-option arguments"

echo "odl_url("${odl_url}")"
echo "client_node("${client_node}")"
echo "server_node("${server_node}")"
echo "rsp_name("${rsp_name}")"

push_nsh
