(ns rockford.db
  (:require [hikari-cp.core :as hikari]
            [clojure.java.jdbc :as j]
            [clojure.set :as set]))

(def local-datasource-options
                         {:connection-timeout 30000
                          :idle-timeout 600000
                          :max-lifetime 1800000
                          :minimum-idle 10
                          :maximum-pool-size  10
                          :adapter "mysql"
                          :username "root"
                          :password "xpass"
                          :database-name "rockford"
                          :server-name "localhost"
                          :port-number 3306})

;; General utilities

(defn rename-ids
  [xs new-id]
  (map #(set/rename-keys % {:id new-id}) xs))

(defn augment-results
  [xs keyname map-query]
  (map #(assoc % keyname (map-query %)) xs))

;; Datasource setup and shutdown

(def local-datasource
  (hikari/make-datasource local-datasource-options))

(defn shutdown-datasource []
    (hikari/close-datasource local-datasource))

;; Functions to pass queries to

(defn do-local-insert!
  [table map]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/insert! conn table map)))

(defn do-local-query [query]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/query conn query)))

(defn do-execute! [query]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/execute! conn query)))

;; Queries



;; Query combiners

(defn add-genes-to-sample
  [sample-map gene-map]
  (->> (map #(assoc % :sample_id (:sample_id sample-map)) gene-map)
    (assoc sample-map :genes)))