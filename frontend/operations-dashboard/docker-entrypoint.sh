#!/bin/sh
set -eu
export API_BASE="${API_BASE:-${NEXT_PUBLIC_API_BASE:-}}"
mkdir -p /app/public
node -e 'const fs=require("fs"); const v=process.env.API_BASE||""; fs.writeFileSync("/app/public/config.js","window.__FULFILLX_API_BASE__="+JSON.stringify(v)+";\n");'
exec node server.js
