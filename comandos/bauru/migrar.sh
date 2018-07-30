#!/bin/sh

user1=$1
admin='admin@fc.unesp.br'
password1=password
host1=zmx.bauru.unesp.br

user2=$2
host2='imap.gmail.com'
password2="/opt/migracao/google_service_account.json"

tmpdir='./tmp'
pidfile="imapsync-${user1}.pid"
mkdir $tmpdit

    imapsync --host1 "$host1" --user1 "$user1" --authuser1 "$admin" --password1 "$password1" \
	--host2 "$host2" --user2 "$user2" --password2 $password2 --authmech2 XOAUTH2 \
        --timeout 600 --reconnectretry1 8 --reconnectretry2 8 --maxsize 25000000 --buffersize 8192000 \
	--tmpdir "$tmpdir" \
	--pidfile "$pidfile" \
	--addheader \
        --automap \
        --nofoldersizes \
        --exclude "^Comments$" --exclude "^Calendar$" --exclude "^Contacts$" --exclude "^Emailed Contacts$" \
        --exclude "^Chats$" --exclude "^Pastas P&APo-blicas" --exclude "^Notes$" --exclude "^Deleted Messages$" \
        --exclude "\[Gmail\]$" \
        --regextrans2 "s,(/|^) +,\$1,g" --regextrans2 "s, +(/|$),\$1,g" --regextrans2 "s/['\^\"\\\\]/_/g" \
        --regextrans2 's/^INBOX\.(.*)/$1/' --regextrans2 's/^INBOX\/(.*)/$1/' \
        --f1f2 'Sent'='[Gmail]/E-mails enviados' --f1f2 'Trash'='[Gmail]/Lixeira' \
        --f1f2 'Drafts'='[Gmail]/Rascunhos' --f1f2 'Junk'='[Gmail]/Spam' --regextrans2 's/^Enviados/Enviados_/' \
        --folderfirst "INBOX" \
        --justfolders
        
    imapsync --host1 "$host1" --user1 "$user1" --authuser1 "$admin" --password1 "$password1" \
	--host2 "$host2" --user2 "$user2" --password2 $password2 --authmech2 XOAUTH2 \
        --timeout 600 --reconnectretry1 8 --reconnectretry2 8 --maxsize 25000000 --buffersize 8192000 \
	--tmpdir "$tmpdir" \
	--pidfile "$pidfile" \
	--addheader \
        --automap \
        --nofoldersizes \
        --exclude "^Comments$" --exclude "^Calendar$" --exclude "^Contacts$" --exclude "^Emailed Contacts$" \
        --exclude "^Chats$" --exclude "^Pastas P&APo-blicas" --exclude "^Notes$" --exclude "^Deleted Messages$" \
        --exclude "\[Gmail\]$" \
        --regextrans2 "s,(/|^) +,\$1,g" --regextrans2 "s, +(/|$),\$1,g" --regextrans2 "s/['\^\"\\\\]/_/g" \
        --regextrans2 's/^INBOX\.(.*)/$1/' --regextrans2 's/^INBOX\/(.*)/$1/' \
        --f1f2 'Sent'='[Gmail]/E-mails enviados' --f1f2 'Trash'='[Gmail]/Lixeira' \
        --f1f2 'Drafts'='[Gmail]/Rascunhos' --f1f2 'Junk'='[Gmail]/Spam' --regextrans2 's/^Enviados/Enviados_/' \
        --folderfirst "INBOX" \
        --maxage 15 \
        --expunge1 --delete1

    imapsync --host1 "$host1" --user1 "$user1" --authuser1 "$admin" --password1 "$password1" \
	--host2 "$host2" --user2 "$user2" --password2 $password2 --authmech2 XOAUTH2 \
        --timeout 600 --reconnectretry1 8 --reconnectretry2 8 --maxsize 25000000 --buffersize 8192000 \
	--tmpdir "$tmpdir" \
	--pidfile "$pidfile" \
	--addheader \
        --automap \
        --nofoldersizes \
        --exclude "^Comments$" --exclude "^Calendar$" --exclude "^Contacts$" --exclude "^Emailed Contacts$" \
        --exclude "^Chats$" --exclude "^Pastas P&APo-blicas" --exclude "^Notes$" --exclude "^Deleted Messages$" \
        --exclude "\[Gmail\]$" \
        --regextrans2 "s,(/|^) +,\$1,g" --regextrans2 "s, +(/|$),\$1,g" --regextrans2 "s/['\^\"\\\\]/_/g" \
        --regextrans2 's/^INBOX\.(.*)/$1/' --regextrans2 's/^INBOX\/(.*)/$1/' \
        --f1f2 'Sent'='[Gmail]/E-mails enviados' --f1f2 'Trash'='[Gmail]/Lixeira' \
        --f1f2 'Drafts'='[Gmail]/Rascunhos' --f1f2 'Junk'='[Gmail]/Spam' --regextrans2 's/^Enviados/Enviados_/' \
        --folderfirst "INBOX" \
        --expunge1 --delete1
