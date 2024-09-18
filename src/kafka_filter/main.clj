(ns kafka-filter.main
  (:gen-class)
  (:require
    [kafka-filter.filter :as filter]
    [kafka-filter.handler :as handler]
    [ring.adapter.jetty :as jetty]))

(defn -main [& [port kafka-server]]
  (jetty/run-jetty #'handler/app {:port (parse-long (or port "3000")) :join? nil})
  (filter/main-loop (or kafka-server "localhost:9092")))
