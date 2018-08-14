#!/bin/sh

# Run fron the root project folder
if [[ -d build-resources ]]
then

set -x
rm -f build-resources/gpg_files.*

gpg -a --export-secret-keys gmcelhoe@vmware.com >gpg-secret-keys
gpg --export-ownertrust >gpg-ownertrust
tar cvf gpg_files.tar gpg-ownertrust gpg-secret-keys
travis encrypt-file gpg_files.tar
mv -f gpg_files.tar.enc build-resources

rm -f gpg-secret-keys gpg-ownertrust gpg_files.tar

else
  echo "Run this script from the root project folder"
fi
