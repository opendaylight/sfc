#!/bin/bash

# Test traffic before and after moving one VM
# to other compute node in a different subnet

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


# Test traffic after moving one VM to other compute node
# - Three Service Function Chains dpi ("Deep Packet Inspection"),
#     qos ("Quality of Service"), ids ("Intrusion Detection System")
# - Remove the configuration and topology
# - Create the network topology all guests in the DIFFERENT subnet
# - Configure sfc
# - Start applications in the guests: basic classifier; SFs and servers
# - Launch traffic
# - Move VM qos to the compute node in which is the dpi
# - Launch traffic
sudo ./exec_logical_sfc.py --odl ${odl_url} -d ../sf_hhe --chains "[['client3', 'dpi, qos, ids', 'server3']]" -rsnt
status=$?
echo "status after testing traffic:" $status

sudo dovs move-guest --name-guest dovs-qos --name-dstnode dpi
status=$?
echo "status after moving one VM (from one host to another with different subnet):" $status

sudo ./exec_logical_sfc.py --odl ${odl_url} -d ../sf_hhe --chains "[['client3', 'dpi, qos, ids', 'server3']]" -t
status=$?
echo "status after testing traffic (once the VM was moved):" $status

exit $status
