# instal twine
pip install twine

# create SFC package

# execute "sudo python3 setup.py sdist" to build the package
python3 setup.py sdist
# check if the package 'sfc-0.1.365.tar.gz' was created in sfc/sfc-py/dist

file="dist/sfc-0.1.4.tar.gz"
if [ -s "$file" ]
then
    echo "Sfc Package created and has size greater than zero."
    # publish the package on PyPI
    # === decided to do this step using Reinaldo Penno login
    USR="rapenno"
    PSW="SfcOdl6105"
    twine upload -u $USR -p $PSW dist/*
else
    echo "Sfc Package not found."
fi

