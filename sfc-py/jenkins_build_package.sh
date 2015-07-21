# openssl-devel MUST BE INSTALLED as it is pip dependency

# instal SSL
apt-get install libssl-dev or  sudo apt-get install libssl-dev openssl

# precondition virtual env must be installed  :  “pip install virtualenv”

# download and extract Python to /tmp
wget https://www.python.org/ftp/python/3.4.3/Python-3.4.3.tgz --directory-prefix=/tmp
tar -xvzf /tmp/Python-3.4.3.tgz -C /tmp
#save current dir should be root of the sfc-py project

curpath="$PWD"

USR="rapenno"
PSW="SfcOdl6105"


cd /tmp/Python-3.4.3

# set Python 3.4 install directory, Python 3.4 executable and virtualenv directory path
sfc_random_suffix=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 10 | head -n 1)
sfc_py3_dir="/tmp/python3_$sfc_random_suffix"
sfc_twine_dir="/tmp/twine_$sfc_random_suffix"
sfc_twine_executable="$sfc_twine_dir/twine"
sfc_py3_executable="$sfc_py3_dir/bin/python3"
sfc_py3_venv_dir="/tmp/sfc_venv_$sfc_random_suffix"

# configure, compile and install Python 3.4
./configure --prefix=$sfc_py3_dir
make
make install

# create and activate virtualenv
$sfc_py3_executable -m venv $sfc_py3_venv_dir
echo ++++++++++++++++ Activating virtualenv +++++++++++++++++++

source "$sfc_py3_venv_dir/bin/activate"

# instal twine
pip install twine 
which twine

# create SFC package

# cd to sfc/sfc-py directory
cd $curpath
 
# 3. execute "sudo python3 setup.py sdist" to build the package
python3 setup.py sdist
# 4. check if the package 'sfc-0.1.365.tar.gz' was created in sfc/sfc-py/dist

file="$curpath/dist/sfc-0.1.365.tar.gz"
if [ -s "$file" ]
then
	echo "Sfc Package created and has size greater than zero."
    # 5. publish the package on PyPI
    # === decided to do this step using Reinaldo Penno login
    #$sfc_twine_executable upload -u $USR -p $PSW dist/*
    twine upload -u $USR -p $PSW dist/*
else
	echo "Sfc Package not found."
fi


# deactivate virtualenv, remove temporary stuff
deactivate
# rm -rf $sfc_py3_dir $sfc_py3_venv_dir $sfc_twine_dir
