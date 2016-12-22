#!/bin/bash

# Test traffic in two Service Function Chains, each SFs in a different subnet

function print_usage {
    echo "Usage:"
    echo " $0 -o <odl_url>"
    echo "Example: "
    echo " $0 -o 172.28.128.3"
}

while getopts o: option
do
    case "${option}"
    in
        o) odl_url=${OPTARG};;
        h  ) print_usage $0; exit;;
        \? ) echo "Unknown option: -${OPTARG}" >&2; exit 1;;
        :  ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
        *  ) echo "Unimplemented option: -$OPTARG" >&2; exit 1;;
    esac
done

if [ $OPTIND -ne 3 ]; then
    echo "Incorrect number of parameters";
    print_usage $0
    exit 1
fi
shift $((OPTIND-1))

echo "odl_url("${odl_url}")"

# Test traffic in two Service Function Chains with the following steps
# - Remove the configuration and topology
# - Create the network topology all guests in the DIFFERENT subnet
# - Configure sfc
# - Start applications in the guests: basic classifier; SFs and servers
# - Launch traffic
sudo ./exec_logical_sfc.py --odl ${odl_url} -d ../sf_hhe --chains "[['client1', 'firewall, napt44', 'server1'], ['client2', 'napt44', 'server2']]" -rsnt
status=$?
echo $status
return $status
