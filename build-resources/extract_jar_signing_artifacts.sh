#!/bin/sh -e

if [ -z "$1" ] || [ -z "$2" ]
then
  echo "Encryption key and initialization vector are required for creating signed jars"

else

  openssl aes-256-cbc -K $encrypted_e586968263a2_key -iv $encrypted_e586968263a2_iv -in build-resources/gpg_files.tar.enc -out gpg_files.tar -d
  tar -f gpg_files.tar -xO gpg-secret-keys | $GPG_EXECUTABLE --import
  tar -f gpg_files.tar -xO gpg-ownertrust | $GPG_EXECUTABLE --import-ownertrust

fi
