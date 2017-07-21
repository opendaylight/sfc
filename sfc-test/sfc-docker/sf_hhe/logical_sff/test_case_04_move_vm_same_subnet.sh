#!/bin/bash

# Test traffic before and after moving one VM
# to other compute node in a SAME subnet

# ./exec_logical_sfc.py and dovs requires sudo
if [ $(id -u) -ne 0 ]; then
   echo "ERROR script has to be run as root"
   exit -1
fi

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

echo "$0 odl_url("${odl_url}")"


# Test traffic after moving one VM to other compute node
# - Three Service Function Chains dpi ("Deep Packet Inspection"),
#     qos ("Quality of Service"), ids ("Intrusion Detection System")
# - Remove the configuration and topology
# - Create the network topology all guests in the SAME subnet
# - Configure sfc
# - Start applications in the guests: basic classifier; SFs; client and server
# - Launch traffic
./exec_logical_sfc.py --odl ${odl_url} -d ../sf_hhe --chains "[['client6', 'dpi, qos, ids', 'server6']]" -rst
status=$?
echo "status after testing traffic:" $status

./GetSfcConfig.py -v -ip ${odl_url}

# - Move VM qos to the compute node in which is the dpi
dovs move-guest --name-guest dovs-qos --name-dstnode dpi
status=$?
echo "status after moving one VM (from one host to another in the same subnet):" $status

# - Launch traffic
./exec_logical_sfc.py --odl ${odl_url} -d ../sf_hhe --chains "[['client6', 'dpi, qos, ids', 'server6']]" -t
status=$?

# Remove sfc configuration and topology
./exec_logical_sfc.py --odl ${odl_url} -d ../sf_hhe --chains "[['client6', 'dpi, qos, ids', 'server6']]" --remove-sfc

echo "status after testing traffic (once the VM was moved):" $status
exit $status
