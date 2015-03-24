__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"

import calendar
from common import *
from sfc_netconf_regression_messages import *
from subprocess import *


# java -Xmx1G -XX:MaxPermSize=256M -jar netconf-testtool-0.3.0-20150320.211342-654-executable.jar

if __name__ == "__main__":

    process = None
    device_name = "sfc-netconf"
    # + str(calendar.timegm(time.gmtime()))

    print("Starting Netconf Server")
    try:
        process = Popen(
            ['java', '-Xmx1G', '-XX:MaxPermSize=256M', '-jar',
             'netconf-testtool-0.3.0-20150320.211342-654-executable.jar', '--device-count', '2'])

        time.sleep(3)

    except CalledProcessError as e:
        print(e.output)

    # input("Press Enter to continue to Auto-Provisioning...")

    try:
        post_netconf_connector(NETCONF_CONNECTOR_URL, NETCONF_CONNECTOR_XML.format(device_name, "localhost"))
    except requests.exceptions.RequestException:
        print("Error sending POST request to spawn connector \n")

    input("Press Enter to check SFF creation...")
    time.sleep(2)
    check(SFF_ONE_URL.format(device_name), SERVICE_FUNCTION_FORWARDER_NETCONF_JSON,
          "Checking if Netconf SFF was created successfully")

    input("Press Enter to finish test...")

    process.kill()
