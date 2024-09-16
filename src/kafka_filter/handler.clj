(ns kafka-filter.handler
  (:require
    [kafka-filter.filter :as f]
    [muuntaja.core :as m]
    [reitit.ring :as r-ring]
    [reitit.ring.middleware.exception :as exception]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [taoensso.timbre :as log]))

(def log-resp-middleware
  {:name ::log-resp-middleware
   :wrap (fn [handler]
           (fn [request]
             (let [resp (handler request)]
               (log/info "log-resp-middleware" resp)
               resp)))})

(defn get-handler [{:keys [params]}]
  (if-let [id (get params "id")]
    (f/get-msgs id)
    (f/get-filters)))

(defn post-handler [{{:keys [topic q]} :body-params}]
  (if (and topic q)
    (f/add-filter topic q)
    {:status 400}))

(defn delete-handler [{:keys [params]}]
  (if-let [id (get params "id")]
    (f/del-filter id)
    {:status 400}))

(def router
  (r-ring/router
    {"/filter" {:get {:handler get-handler}
                :post {:handler post-handler}
                :delete {:handler delete-handler}}}
    {;:reitit.middleware/transform dev/print-request-diffs
     :data {:muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         exception/exception-middleware
                         log-resp-middleware]}}))

(def app
  (r-ring/ring-handler router
                       (r-ring/create-default-handler)))

(defn init [])
