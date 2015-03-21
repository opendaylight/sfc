__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"

from common import *
from sfc_netconf_regression_messages import *
from subprocess import *

if __name__ == "__main__":

    process = None

    try:
        process = Popen(
            ['java', '-Xmx1G', '-XX:MaxPermSize=256M', '-jar',
             'netconf-testtool-0.3.0-20150320.211342-654-executable.jar'])

        time.sleep(2)

    except CalledProcessError as e:
        print(e.output)

    post_netconf_connector(NETCONF_CONNECTOR_URL, NETCONF_CONNECTOR_XML.format("localhost"))

    process.kill()
