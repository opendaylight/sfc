#!/usr/bin/env python

from ast import literal_eval
from sets import Set

import ast
import os
import sys
import time
import commands
import shlex, subprocess
import argparse

dir_logs ='./logs/'


def createListChains(input_chains):
    list_chains = []
    chains = ast.literal_eval(input_chains)
    for chain in chains:
        list_chain_elems = []
        if (len(chain) != 3):
            raise ValueError('Incorrect number of chain parameters')
        else:
            for elem_chain in chain:
                elem_item_split = [n.strip() for n in elem_chain.split(',')]
                if (len(elem_item_split) < 1):
                    raise ValueError('Incorrect number of elements in a chain')
                list_chain_elems.append(elem_item_split)
        list_chains.append(list_chain_elems)
    return list_chains

def getRspNameFromId(chain_id):
    return "RSP" + str(chain_id)

def execCommand(cmd):
    (status, output) = commands.getstatusoutput(cmd)
    print("cmd(" + str(cmd) + ") status(" + str(status) + ") output(" + str(output) + ")")
    return status

def execCmdBackground(cmd, file_stdout):
    args = shlex.split(cmd)
    print args
    print("cmd(" + str(cmd) + ")")
    with open(file_stdout, "w") as outfile:
        p = subprocess.Popen(args, stdout=outfile)
        print("p(" + str(p) + ")")

def execCmdBackgroundShellTrue(cmd, file_stdout):
    print("cmd(" + str(cmd) + ")")
    with open(file_stdout, "w") as outfile:
        p = subprocess.Popen(cmd, stdout=outfile, shell=True)
        print("p(" + str(p) + ")")

#sudo ip netns exec dovs-napt44 python3 ./sf_hhe.py -i eth0 --name napt44 --port 8000 > napt44_sf_hhe.txt &
def execSfHhe(cmd_sf_hhe, name):
    cmd='ip netns exec dovs-' + name + ' ' + cmd_sf_hhe + ' -i eth0 --name ' + name + ' --port 8000'
    #return execCmdBackground(cmd, dir_logs + name + '_sf_hhe.txt')
    return execCmdBackgroundShellTrue(cmd, dir_logs + name + '_sf_hhe.txt')

#sudo ip netns exec dovs-server1 python ./HttpServer.py > dovs-server1.txt &
def execHttpServer(cmd_http_server, name):
    cmd='ip netns exec dovs-' + name + ' python ' + cmd_http_server
    #return execCmdBackground(cmd, dir_logs + name + '_http_server.txt')
    return execCmdBackgroundShellTrue(cmd, dir_logs + name + '_http_server.txt')

#sudo ip netns exec dovs-server1 tcpdump -i eth0 -wU client1_tcpdump.pcap
def execTcpdump(guest):
    cmd='ip netns exec dovs-' + guest + ' tcpdump -i eth0 -U -w ' + dir_logs + guest + '_tcpdump.pcap'
    killProcess(cmd)
    #return execCmdBackground(cmd, dir_logs + guest + '_tcpdump.txt')
    return execCmdBackgroundShellTrue(cmd, dir_logs + guest + '_tcpdump.txt')

#sudo tcpdump -i dovs-tun -U -w dovs-tun.pcap
def execTcpdumpDovsTun():
    cmd='tcpdump -i dovs-tun -U -w ' + dir_logs + 'dovs-tun.pcap'
    killProcess(cmd)
    return execCmdBackgroundShellTrue(cmd, dir_logs + 'dovs-tun.pcap')
def killTcpdumpDovsTun():
    cmd='tcpdump -i dovs-tun -U -w ' + dir_logs + 'dovs-tun.pcap'
    killProcess(cmd)

#server_ip=$(sudo ip netns exec dovs-${server_node} ip addr sh | grep -A 2 ": eth0" | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)')
#sudo ip netns exec dovs-client1 python3 ./HttpClient.py -ip ${server_ip}
def getCmdHttpClient(cmd_http_client, client, server, chain):
    str_chain = str(chain)
    print("str_chain(" + str_chain + ")")
    (status, output_server_ip) = commands.getstatusoutput('ip netns exec dovs-' + server + " ip addr sh | grep -A 2 ': eth0' | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)'")
    cmd='ip netns exec dovs-' + client + ' python3 ' + cmd_http_client + ' -ip ' + output_server_ip + ' --chain \"' + str_chain + '\"'
    return cmd

#server_ip=$(sudo ip netns exec dovs-${server_node} ip addr sh | grep -A 2 ": eth0" | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)')
#sudo ip netns exec dovs-client1 ping -c 1 ${server_ip}
def getCmdPing(client, server, number):
    (status, output_server_ip) = commands.getstatusoutput('ip netns exec dovs-' + server + " ip addr sh | grep -A 2 ': eth0' | tail -n 1 | grep -o -P '(?<=inet ).*(?=/)'")
    cmd='ip netns exec dovs-' + client + ' ping -c ' + str(number) + ' ' + output_server_ip
    return cmd

#./push_nsh.sh -o 172.28.128.31 -c client1 -s server1 -r RSP1
def execPushNsh(cmd_push_nsh, url_odl, client, server, chain_id):
    rsp_name = getRspNameFromId(chain_id)
    cmd=cmd_push_nsh + ' -o ' + url_odl + ' -c ' + client + ' -s ' + server + ' -r ' + rsp_name
    execCommand(cmd)

#ps -ef | grep "sf_hhe.py" | grep -v grep | awk '{print $2}' | xargs sudo kill
def killProcess(cmd):
    cmd='ps -ef | grep "' + cmd + '"' + " | grep -v grep | awk '{print $2}'"
    (status, output) = commands.getstatusoutput(cmd)
    if (output != ""):
        execCommand(cmd + " | xargs kill")

#docker exec firewall ovs-ofctl -O OpenFlow13 dump-flows br-int > firewall_dump_flows.txt
def exec_dump_flows(chains):
    set_guest_nodes = Set([])
    for sfc in chains:
        for guests in sfc:
            for guest in guests:
                set_guest_nodes.add(guest)
    for guest in set_guest_nodes:
        cmd=('docker exec ' + guest + ' ovs-ofctl -O OpenFlow13 dump-flows br-int > '
            + dir_logs + guest + '_dump_flows.txt')
        execCommand(cmd)

#docker exec firewall ovs-ofctl -O OpenFlow13 show br-int > firewall_ofctl_show.txt
def exec_ofctl_show(chains):
    set_guest_nodes = Set([])
    for sfc in chains:
        for guests in sfc:
            for guest in guests:
                set_guest_nodes.add(guest)
    for guest in set_guest_nodes:
        cmd=('docker exec ' + guest + ' ovs-ofctl -O OpenFlow13 show br-int > '
            + dir_logs + guest + '_ofctl_show.txt')
        execCommand(cmd)

#docker exec firewall ovs-vsctl -O OpenFlow13 show br-int > firewall_vsctl_show.txt
def exec_vsctl_show(chains):
    set_guest_nodes = Set([])
    for sfc in chains:
        for guests in sfc:
            for guest in guests:
                set_guest_nodes.add(guest)
    for guest in set_guest_nodes:
        cmd=('docker exec ' + guest + ' ovs-vsctl show > '
            + dir_logs + guest + '_vsctl_show.txt')
        execCommand(cmd)

def execAllCmds(args_dir, chains, url_odl, execute_traffic):
    status = 0
    cmd_push_nsh ='./push_nsh.sh'
    cmd_http_client = args_dir + '/HttpClient.py'
    cmd_http_server = args_dir + '/HttpServer.py'
    cmd_sf_hhe = args_dir + '/sf_hhe.py'

    print ('Current time: ' + time.asctime(time.localtime(time.time())))

    execCommand('mkdir -p ' + dir_logs)

    killProcess(cmd_http_server)
    killProcess(cmd_sf_hhe)
    chainTypes = []
    http_client_cmds = []
    ping_cmds = []
    chain_id = 0

    execTcpdumpDovsTun()

    for sfc in chains:
        chain_id += 1
        client = (sfc[0])[0]
        chain = sfc[1]
        server = (sfc[2])[0]
        execTcpdump(client)
        execTcpdump(server)
        # Test direct connection between client-server without NSH
        if (execute_traffic):
            #status += execCommand(getCmdPing(client, server, 3))
            execCommand(getCmdPing(client, server, 2))
        execHttpServer(cmd_http_server, server)
        #execPushNsh(cmd_push_nsh, url_odl, client, server, chain_id)
        for c in chain:
            chainTypes.append(c)
        http_client_cmds.append(getCmdHttpClient(
            cmd_http_client, client, server, chain))
        ping_cmds.append(getCmdPing(client, server, 1))
    chainTypes = set(chainTypes)
    for c in chainTypes:
        execTcpdump(c)
        execSfHhe(cmd_sf_hhe, c)
    seconds = 2
    print ("Waiting " + str(seconds) + " seconds ...")
    print ('Current time: ' + time.asctime(time.localtime(time.time())))
    time.sleep(seconds)
    for cmd in ping_cmds:
        if (execute_traffic):
            status += execCommand(cmd)
        else:
            print ('Ping command suggestion:  ' + cmd)
    for cmd in http_client_cmds:
        if (execute_traffic):
            status += execCommand(cmd)
        else:
            print ('Traffic command suggestion:  ' + cmd)
    exec_dump_flows(chains)
    exec_ofctl_show(chains)
    exec_vsctl_show(chains)
    print ('Current time: ' + time.asctime(time.localtime(time.time())))
    print ("To kill all in the namespaces:  sudo ip netns list | grep dovs | xargs -l1 sudo ip netns pids | xargs -l1 sudo kill")
    print ("Logs stored in " + dir_logs)

    killTcpdumpDovsTun()

    return status

def sfcConfig(args_chains, args_odl, args_different_subnets, args_only_config, args_aio):
    print ("Creating SFC data ...")
    # sudo dovs sfc-config
    cmd = 'dovs sfc-config --chains "' + args_chains + '" --odl ' + args_odl
    if (args_different_subnets):
        cmd = cmd + ' --different-subnets'
    if (args_only_config):
        cmd = cmd + ' --only-config'
    if (args_aio):
        cmd = cmd + ' --allinonenode'
    execCommand(cmd)
    seconds = 2
    print ("Waiting " + str(seconds) + " seconds ...")
    time.sleep(seconds)

def execRemove(chains, args_odl):
    deleteRsps(chains, args_odl)
    # The following commands requires 'sudo'
    execCommand("ip netns list | grep dovs | xargs -l1 ip netns pids | xargs -l1 kill")
    execCommand("dovs clean --odl " + args_odl)
    execCommand("docker rm $(docker stop $(docker ps -q -a))")
    execCommand("dovs info")
    seconds = 2
    print ("Waiting " + str(seconds) + " seconds ...")
    time.sleep(seconds)

def createRsps(chains, args_odl):
    chain_id = 0
    for sfc in chains:
        chain_id += 1
        #'sudo dovs sfc-config'
        execCommand(" dovs sfc-config --create-rsp-from-id " + str(chain_id) + " --odl " + args_odl)
    time.sleep(1)

def deleteRsps(chains, args_odl):
    chain_id = 0
    for sfc in chains:
        chain_id += 1
        #'sudo dovs sfc-config'
        execCommand("dovs sfc-config --delete-rsp-from-id " + str(chain_id) + " --odl " + args_odl)
    time.sleep(1)

if __name__ == "__main__":
    status = 0
    if os.geteuid() != 0:
        print('This tool can only be run as root')
        os._exit(1)

    args_chain_example="""--chains \"[[\'client1\', \'firewall, napt44\', \'server1'], [\'client2\', \'napt44\', \'server2\']]\""""
    parser = argparse.ArgumentParser(description='Python3 script start Logical Service Function Chaining execution',
        usage="""%(prog)s [options]
Example:
sudo ./%(prog)s --odl 172.28.128.3 -d ../sf_hhe """ + args_chain_example + " -rsnt",
        add_help=True)
    parser.add_argument('-o', '--odl',
                        help='ODL Controller URL')
    parser.add_argument('-d', '--dir-sf',
                        help='Directory path in which the sf_hhe execution scripts are located')
    parser.add_argument('-c', '--chains',
                        help="""Example: --chains "[['client1', 'firewall, napt44', 'server1'], ['client2', 'napt44', 'server2']]" """)
    parser.add_argument('-r', '--remove-sfc', action='store_true', default=False,
        help='Remove the configuration and topology')
    parser.add_argument('-s', '--create-sfc', action='store_true', default=False,
        help='Create the network topology and configure sfc')
    parser.add_argument('-f', '--only-config', action='store_true', default=False,
        help='When creating SFC using --create-sfc, configure SFC and its RSPs (without creating the network topology)')
    parser.add_argument('-n', '--different-subnets', action='store_true', default=False,
        help='When creating the network topology, add guests to different subnets')
    parser.add_argument('-a', '--exec-apps', action='store_true', default=False,
        help='Start applications in the guests: basic classifier; SFs and servers')
    parser.add_argument('-t', '--exec-traffic', action='store_true', default=False,
        help='Start applications and also launch traffic')
    parser.add_argument('--aio', action='store_true', default=False,
        help='Use a single node topology')

    args = parser.parse_args()
    if ((args.odl is None) or (args.dir_sf is None) or (args.chains is None)):
        parser.print_help()
        sys.exit(-1)

    try:
        chains = createListChains(args.chains)
    except (ValueError, SyntaxError) as exceptError:
        print("""Chains format is not correct ({}), example: """ + args_chain_example,
            exceptError)
        os._exit(1)

    print("args.dir_sf(" + str(args.dir_sf) + ")")
    print("chains(" + str(chains) + ")")
    print("args.odl(" + str(args.odl) + ")")
    print("args.remove_sfc(" + str(args.remove_sfc) + ")")
    print("args.create_sfc(" + str(args.create_sfc) + ")")
    print("args.only_config(" + str(args.only_config) + ")")
    print("args.different_subnets(" + str(args.different_subnets) + ")")
    print("args.exec_apps(" + str(args.exec_apps) + ")")
    print("args.exec_traffic(" + str(args.exec_traffic) + ")")
    print("args.aio(" + str(args.aio) + ")")

    if args.remove_sfc:
        execRemove(chains, args.odl)
    if args.create_sfc:
        sfcConfig(args.chains, args.odl, args.different_subnets, args.only_config, args.aio)
        createRsps(chains, args.odl)
    if (args.exec_apps or args.exec_traffic):
        status += execAllCmds(args.dir_sf, chains, args.odl, args.exec_traffic)

    print("status(" + str(status) + ")")
    if (not (status == 0)):
        os._exit(-1)
