# import calendar
import common
import sfc_netconf_regression_messages as sfc_nrm
import subprocess
import argparse
import time
import requests
import urllib2
import os

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

def download_netconf_jar():
    url='https://nexus.opendaylight.org/service/local/artifact/maven/redirect?r=opendaylight.release&g=org.opendaylight.controller&a=netconf-testtool&v=0.3.0-Lithium&e=jar&c=executable'
    # In the future, maybe use one of the following URLs instead
    #url='https://nexus.opendaylight.org/service/local/artifact/maven/redirect?r=opendaylight.release&g=org.opendaylight.controller&a=netconf-testtool&v=0.3.4-Lithium-SR4&e=jar&c=executable'
    #url='https://nexus.opendaylight.org/service/local/artifact/maven/redirect?r=opendaylight.snapshot&g=org.opendaylight.netconf&a=netconf-testtool&v=1.5.0-SNAPSHOT&e=jar&c=executable'
    attempts = 0

    while attempts < 3:
        try:
            response = urllib2.urlopen(url, timeout = 5)
            # Get the local filename based on the URL
            url_list = response.geturl().split('/')
            localfile = url_list[len(url_list)-1]
            content_length = response.info()['Content-Length']

            print "Downloading %s bytes to: %s" % (content_length, localfile)
            print "From: %s" % response.geturl()

            if (os.path.exists(localfile) and os.path.isfile(localfile)):
                count = os.path.getsize(localfile)
                print "\nFile already downloaded, size %d bytes\n" % count
                break

            content = response.read()
            f = open(localfile, 'w')
            f.write(content)
            f.close()
            break
        except urllib2.URLError as e:
            attempts += 1
            print type(e)


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

    download_netconf_jar()

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
