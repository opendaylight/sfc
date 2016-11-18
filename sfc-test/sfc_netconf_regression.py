# import calendar
import common
import sfc_netconf_regression_messages as sfc_nrm
import subprocess
import argparse
import time
import requests

__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.3"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"

# java -Xmx1G -XX:MaxPermSize=256M -jar \
# netconf-testtool-0.3.0-20150320.211342-654-executable.jar


def run_netconf_tests():
    process = None
    device_name = "sfc-netconf"
    print("Starting Netconf Server")
    try:
        process = subprocess.Popen(
            ['java', '-Xmx1G', '-XX:MaxPermSize=256M', '-jar',
             'netconf-testtool-0.3.0-20150320.211342-654-executable.jar',
             '--device-count', '2'])

        time.sleep(5)

    except subprocess.CalledProcessError as e:
        print(e.output)
        return

    # input("Press Enter to continue to Auto-Provisioning...")

    try:
        common.post_netconf_connector(
            common.NETCONF_CONNECTOR_URL,
            sfc_nrm.NETCONF_CONNECTOR_XML.format(device_name, "localhost"))
        time.sleep(5)
        common.check(
            common.SFF_ONE_URL.format(device_name),
            sfc_nrm.SERVICE_FUNCTION_FORWARDER_NETCONF_JSON,
            "Checking if Netconf SFF was created successfully")
        input("Press Enter to finish tests")
    except requests.exceptions.RequestException:
        print("Error sending POST request to spawn netconf connector \n")
    finally:
        print("Finishing Tests...")
        process.kill()

    return


def main():

    run_karaf = False
    p = None

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(
        description='SFC Basic RestConf Regression',
        usage=("\npython3.4 sfc_netconf_regression --run-karaf "))

    parser.add_argument('--run-karaf', action='store_true',
                        help='Create SFC Karaf instance automatically')

    #: parse CMD arguments ----------------------------------------------------
    args = parser.parse_args()

    if args.run_karaf:
        run_karaf = True

    # + str(calendar.timegm(time.gmtime()))

    try:

        if run_karaf:
            p = common.initialize_karaf()
            if common.check_sfc_initialized(p):
                run_netconf_tests()
                p.terminate(force=True)
            else:
                print("Bypassing tests..")

        # bundle_pattern = 'list \| grep sfc-netconf\s+\w+\s\|\s(\w+)(.+)'
        # child.expect('opendaylight-user', timeout=10)
        # child.sendline('bundle:list | grep sfc-netconf')
        # child.expect(bundle_pattern)
        # netconf_state, right = child.match.groups()

        else:
            run_netconf_tests()

    except KeyboardInterrupt:
        pass
    finally:
        if p:
            p.terminate(force=True)


if __name__ == "__main__":
    main()
