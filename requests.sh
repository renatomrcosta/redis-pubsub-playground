#bin/bash

for i in {1..20} ; do
  curl -X POST --location "http://localhost:8080/producer/single" \
    -H "Accept: application/json" &
done
