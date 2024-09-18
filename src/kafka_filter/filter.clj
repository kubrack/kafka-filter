(ns kafka-filter.filter
  (:require
    [taoensso.timbre :as log])
  (:import
    [java.time Duration]
    [org.apache.kafka.clients.consumer KafkaConsumer]
    [org.apache.kafka.common.serialization StringDeserializer]))

(def kafka-server "localhost:9092") ; TODO get it from cli opts/env

(def filter-id (atom 0))
(def filters (agent {})) ; {filter-key {:topic topic :q q :msgs [msgs]}}
(def filters-by-topic (agent {})) ; {topic-key #{filter-ids}}

(def consumer
  (KafkaConsumer. {"bootstrap.servers",  kafka-server
                   "group.id",           "example"
                   "key.deserializer",   StringDeserializer
                   "value.deserializer", StringDeserializer
                   "auto.offset.reset",  "earliest"
                   "enable.auto.commit", "true"}))

(defn pred-by-q [q]
  (let [re (re-pattern (str "(?i)" q))]
   (fn [val] (re-find re val))))

(defn process-msgs [records]
  (->> records
       (map (fn [r] [(.topic r) (.value r)]))
       ((fn [r] (when (seq r) (log/debug "Got" (count r) "new records")) r))
       (group-by first)
       (map (fn [[topic pairs]] [topic (map second pairs)]))
       (map (fn [[topic values]]
              (let [checkers (->> (keyword topic)
                                  (get @filters-by-topic)
                                  (map #(get-in @filters [% :checker]))
                                  (into []))]
                (doseq [checker checkers]
                  (let [matched (filter checker values)
                        filter-id (checker)]
                    (when (seq matched)
                      (log/debug (count matched) "records matched filter" filter-id)
                      (send filters update-in [filter-id :msgs] #(concat % matched))))))))
       doall))

(defn main-loop []
  (while 1
    (let [topics (->> @filters vals (map :topic))]
      (.subscribe consumer topics)
      (if (seq topics)
        (process-msgs (.poll consumer (Duration/ofMillis 1000))) ;Long/MAX_VALUE))
        (Thread/sleep 1000)))))

;; API handlers

(defn get-msgs [id]
  (if-let [f (->> id str keyword (get @filters))]
    {:status :ok :result (:msgs f)}
    {:status "id not found"}))

(defn get-filters []
  {:status :ok
   :result (->> @filters
                (map (fn [[k v]] [k (dissoc v :msgs :checker)]))
                (into {}))})

(defn del-filter [id]
  (let [f-key (-> id str keyword)
        current-filters @(send filters dissoc f-key)
        topic (-> current-filters (get f-key) :topic )
        topic-key (keyword topic)]
    (if topic-key
      (do
        (send filters-by-topic
              (fn [current-topic-key->filters]
                (let [t-filters (-> current-topic-key->filters
                                    (get topic-key)
                                    (disj f-key))]
                  (if (-> current-topic-key->filters topic-key seq)
                    (assoc current-topic-key->filters topic-key t-filters)
                    (dissoc current-topic-key->filters topic-key)))))
        {:status :ok})
      {:status "id not found"})))

(defn add-filter [topic q]
  (let [id (swap! filter-id inc)
        f-key (-> id str keyword)
        checker (fn ([] f-key) ([s] ((pred-by-q q) s)))]
    (send filters assoc f-key {:topic topic :q q
                               :msgs []
                               :checker checker})
    (send filters-by-topic
          (fn [current-topic-key->filters]
            (let [t-key (keyword topic)
                  t-filters (-> current-topic-key->filters
                                (get t-key)
                                (->> (into #{}))
                                (conj f-key))]
              (assoc current-topic-key->filters t-key t-filters))))
    {:status :ok :result {:topic topic :q q :id id}}))

