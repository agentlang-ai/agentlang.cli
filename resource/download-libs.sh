
#!/usr/bin/env bash

mkdir lib
curl -L -o install.sh https://github.com/muazzam0x48/sqlite-vec/releases/download/latest/install.sh
chmod +x install.sh
./install.sh
rm install.sh
mv vec0* lib/