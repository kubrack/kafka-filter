# kafka-filter

HTTP API to filter messages from Kafka

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running
on default web-API port `3000`, connect to default kafka `localhost:9092`
```
lein run
```
or
```
lein run web-API-listen-port
```
or
```
lein run web-API-listen-port kafka-host:port
```
# Request examples
```
curl --request POST --url 'http://localhost:3000/filter' --data '{"topic": "books", "q": "sicp"}' --header 'Content-Type: application/json'
> {"status":"ok","result":{"topic":"books","q":"sicp","id":1}}

curl --request GET --url 'http://localhost:3000/filter?id=1'
> {"status":"ok","result":[]}

curl --request GET --url 'http://localhost:3000/filter?id=222'
> {"status":"id not found"}

kcat -b localhost -P -t 'books'
SICP 1996
Of Mice and Men
FP-ML Harrison
^D

curl --request GET --url 'http://localhost:3000/filter?id=1'
> {"status":"ok","result":["SICP 1996"]}

curl --request GET --url 'http://localhost:3000/filter'
> {"status":"ok","result":{"1":{"topic":"books","q":"sicp"}}}

curl --request DELETE --url 'http://localhost:3000/filter?id=1'
> {"status":"ok"}

curl --request DELETE --url 'http://localhost:3000/filter?id=1'
> {"status":"id not found"}
```
## Testing
```
lein test
```
## License

Copyright © 2024 FIXME
