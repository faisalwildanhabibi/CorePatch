# Ensure module directory and log file are writable by all processes (e.g. system_server)
touch "$MODPATH/zygisk_core.log"
chmod 666 "$MODPATH/zygisk_core.log"
chmod 777 "$MODPATH"
