#!/bin/bash

email_unidade=$1

email=$(ssh zimbra@zmx.bauru.unesp.br zmprov sa zimbraMailDeliveryAddress=$email_unidade)

[[ "$email_unidade" == "$email" ]] && exit 0

email=$(ssh zimbra@zmx.bauru.unesp.br zmprov sa zimbraMailAlias=$email_unidade)

if [ ! -z "$email" ]; then
    echo "Alias de $email"
    exit 1
fi

echo "Desconhecido"
exit 1
