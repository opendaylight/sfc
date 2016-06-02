curl -i -H "Content-Type: application/json" -H "Cache-Control: no-cache" --data '{ "input" : { "generation-algorithm" : "sequential" }}' -X POST --user admin:admin http://localhost:8181/restconf/operations/service-path-id:set-generation-algorithm

echo ""

