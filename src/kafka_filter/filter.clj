(ns kafka-filter.filter
  (:require
    [clojure.core.async :as a]
    [taoensso.timbre :as log])
  (:import
    [java.time Duration]
    [org.apache.kafka.clients.consumer KafkaConsumer]
    [org.apache.kafka.common.serialization StringDeserializer]))

(def pool-timeout-ms 1000)
(def ch-buf-len 1000)

(def filter-id (atom 0))
(def filters (agent {}))

(def msg-ch (a/chan ch-buf-len))
(def pub-ch (a/pub msg-ch :topic))

(defn mk-consumer [kafka-server]
  (KafkaConsumer. {"bootstrap.servers",  kafka-server
                   "group.id",           "example"
                   "key.deserializer",   StringDeserializer
                   "value.deserializer", StringDeserializer
                   "auto.offset.reset",  "earliest"
                   "enable.auto.commit", "true"}))

(defn pred-by-q [q]
  (let [re (re-pattern (str "(?i)" q))]
   (fn [val] (re-find re val))))

(defn main-loop [kafka-server]
  (let [consumer (mk-consumer kafka-server)]
    (while 1
      (let [topics (->> @filters vals (map :topic) (into #{}))]
        (.subscribe consumer topics)
        (if (seq topics)
          (let [n (->> (.poll consumer (Duration/ofMillis pool-timeout-ms))
                       (map (fn [r] (a/>!! msg-ch {:topic (-> r .topic keyword)
                                                   :msg   (.value r)})))
                       count)]
            (when (not (zero? n)) (log/debug "Got" n "new msgs")))
          (Thread/sleep pool-timeout-ms))))))

;; API handlers

(defn get-msgs [id]
  (if-let [f (->> id str keyword (get @filters))]
    {:status :ok :result (:msgs f)}
    {:status "id not found"}))

(defn get-filters []
  {:status :ok
   :result (->> @filters
                (map (fn [[k v]] [k (dissoc v :msgs :chan)]))
                (into {}))})

(defn del-filter [id]
  (let [f-key (-> id str keyword)
        current-filters @(send filters dissoc f-key)
        topic (get-in current-filters [f-key :topic])
        topic-ch (get-in current-filters [f-key :chan])]
    (if topic
      (do
        (a/unsub pub-ch (keyword topic) topic-ch)
        {:status :ok})
      {:status "id not found"})))

(defn add-filter [topic q]
  (let [id (swap! filter-id inc)
        f-key (-> id str keyword)
        checker (pred-by-q q)
        topic-ch (a/chan ch-buf-len (comp (map :msg) (filter checker)))]
    (send filters assoc f-key {:topic topic
                               :q q
                               :msgs []
                               :chan topic-ch})
    (a/sub pub-ch (keyword topic) topic-ch)
    (a/go-loop []
      (when-let [msg (a/<! topic-ch)]
          (log/debug "Got a msg matched filter" f-key ":" msg)
          (send filters update-in [f-key :msgs] #(conj % msg)))
          (recur))
    {:status :ok :result {:topic topic :q q :id id}}))

