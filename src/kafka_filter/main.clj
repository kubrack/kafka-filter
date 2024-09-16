(ns kafka-filter.main
  (:gen-class)
  (:require
    [kafka-filter.filter :as filter]
    [kafka-filter.handler :as handler]
    [ring.adapter.jetty :as jetty]))

(defn -main [& _args]
  (jetty/run-jetty #'handler/app {:port 3001 :join? nil})
  (filter/main-loop))
