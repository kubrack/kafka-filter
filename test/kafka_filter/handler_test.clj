(ns kafka-filter.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as j]
            [ring.mock.request :as mock]
            [kafka-filter.handler :as h]))

(deftest test-app
  (testing "post 1"
    (let [req (mock/request :post "/filter")
          response (h/app (mock/json-body req {:topic "books", :q "sicp 1984"}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok",
                   "result" {"q" "sicp 1984", "id" 1, "topic" "books"}}))))

  (testing "post 2"
    (let [req (mock/request :post "/filter")
          response (h/app (mock/json-body req {:topic "books", :q "sicp 1996"}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok"
                   "result" {"id" 2, "topic" "books", "q" "sicp 1996"}}))))

  (testing "post 3"
    (let [req (mock/request :post "/filter")
          response (h/app (mock/json-body req {:topic "books", :q "sicp 2022 js"}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok"
                   "result" {"id" 3, "topic" "books", "q" "sicp 2022 js"}}))))

  (testing "post Bad Request"
    (let [req (mock/request :post "/filter")
          response (h/app (mock/json-body req {:topic "books"}))]
      (is (= (:status response) 400))
      (is (nil? (:body response)))))

  (testing "get list 1"
    (let [response (h/app (mock/request :get "/filter"))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok",
                   "result" {"1" {"topic" "books", "q" "sicp 1984"}
                             "2" {"topic" "books", "q" "sicp 1996"}
                             "3" {"topic" "books", "q" "sicp 2022 js"}}}))))

  (testing "delete"
    (let [response (h/app (mock/request :delete "/filter" {:id 3}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok"}))))

  (testing "get list 2"
    (let [response (h/app (mock/request :get "/filter"))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok",
                   "result" {"1" {"topic" "books", "q" "sicp 1984"}
                             "2" {"topic" "books", "q" "sicp 1996"}}}))))

  (testing "delete Bad Request"
    (let [response (h/app (mock/request :delete "/filter"))]
      (is (= (:status response) 400))
      (is (nil? (:body response)))))

  (testing "delete id not found"
    (let [response (h/app (mock/request :delete "/filter" {:id 3}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "id not found"}))))

  (testing "get msgs"
    (let [response (h/app (mock/request :get "/filter" {:id 1}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "ok", "result" []}))))

  (testing "get msgs id not found"
    (let [response (h/app (mock/request :get "/filter" {:id 3}))
          body (-> response :body j/read-value)]
      (is (= (:status response) 200))
      (is (= body {"status" "id not found"}))))

  (testing "not-found route"
    (let [response (h/app (mock/request :get "/"))]
      (is (= (:status response) 404)))))
