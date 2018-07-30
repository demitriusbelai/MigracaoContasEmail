#!/bin/bash

email_unidade=$1
email_unesp=$2

ssh zimbra@zmx.bauru.unesp.br \
    zmprov ma $email_unidade zimbraPrefMailForwardingAddress $email_unesp zimbraPrefMailLocalDeliveryDisabled TRUE
