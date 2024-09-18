(ns kafka-filter.handler
  (:require
    [kafka-filter.filter :as f]
    [muuntaja.core :as m]
    [reitit.ring :as r-ring]
    ;[reitit.ring.middleware.dev :as dev]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [taoensso.timbre :as log]))

(def log-resp-middleware
  {:name ::log-resp-middleware
   :wrap (fn [handler]
           (fn [request]
             (let [resp (handler request)]
               (log/debug resp)
               resp)))})

(defn get-handler [{:keys [params]}]
  (if-let [id (get params "id")]
    {:status 200 :body (f/get-msgs id)}
    {:status 200 :body (f/get-filters)}))

(defn post-handler [{{:keys [topic q]} :body-params}]
  (if (and topic q)
    {:status 200 :body (f/add-filter topic q)}
    {:status 400}))

(defn delete-handler [{:keys [params]}]
  (if-let [id (get params "id")]
    {:status 200 :body (f/del-filter id)}
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
                         log-resp-middleware]}}))

(def app
  (r-ring/ring-handler router
                       (r-ring/create-default-handler)))
