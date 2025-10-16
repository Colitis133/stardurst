#!/system/bin/sh
export BRIDGE_SECRET_FILE=/data/data/com.stardust/.bridge_secret
cd /data/data/com.stardust/runtime
# Ensure dependencies installed (only first run)
[ -d node_modules ] || npm ci --only=production
# Decrypt creds if stored encrypted
# start launcher
node launcher.js 2>&1 | tee logs/stdout.log
