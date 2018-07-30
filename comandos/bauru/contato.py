#!/usr/bin/python2

import sys
import csv
import subprocess
from StringIO import StringIO


cvsoutput = subprocess.check_output(["ssh", "zimbra@zmx.bauru.unesp.br", "zmmailbox", "-z", "-m", sys.argv[1], "gru", "/Contacts"])
buff = StringIO(cvsoutput)

reader = csv.DictReader(buff)
out = csv.writer(sys.stdout)

for line in reader:
    if not 'email' in line or line['email'] == '':
        continue
    if not 'firstName' in line:
        line['firstName'] = ''
    if not 'lastName' in line:
        line['lastName'] = ''
    if not 'fullName' in line:
        line['fullName'] = ''
    out.writerow([line['email'], line['firstName'], line['lastName'], line['fullName']])
