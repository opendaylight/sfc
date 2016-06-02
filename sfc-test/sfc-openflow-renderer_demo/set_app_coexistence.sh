curl -i -H "Content-Type: application/json" -H "Cache-Control: no-cache" --data '{ "sfc-of-renderer-config" : { "sfc-of-table-offset" : 20, "sfc-of-app-egress-table-offset" : 80 }}' -X PUT --user admin:admin http://localhost:8181/restconf/config/sfc-of-renderer:sfc-of-renderer-config

echo ""

