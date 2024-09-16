(defproject kafka-filter "0.1.0-SNAPSHOT"
  :description "HTTP API to filter messages from Kafka"
  :url "https://github.com/kubrack/kafka-filter"
  :min-lein-version "2.0.0"
  :dependencies [[com.taoensso/timbre "6.5.0"]
                 [metosin/reitit "0.7.2"]
                 [org.apache.kafka/kafka-clients "3.8.0"]
                 [org.clojure/clojure "1.11.1"]
                 [ring/ring-jetty-adapter "1.10.0"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler kafka-filter.handler/app}
  :main kafka-filter.main
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
