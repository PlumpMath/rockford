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
  [table xs]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/insert! conn table xs)))

(defn do-local-insert-multi!
  [table maps]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/insert-multi! conn table maps)))

(defn do-local-query [query]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/query conn query)))

(defn do-execute! [query]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/execute! conn query)))

;; Queries

(defn get-reference-sequence
  [sequence-id]
  (do-local-query ["SELECT * FROM reference_sequence where id = ?" sequence-id]))

(defn insert-reference-drms!
  [sequence-id sequence start-codon end-codon]
  (let [drm-maps (map #(hash-map :reference_id % :codon_id %2 :sequence %3 :is_drm %4)
                      (repeat (- (inc end-codon) start-codon) sequence-id)
                      (range start-codon (inc end-codon))
                      (map #(apply str %) (partition 3 sequence))
                      (repeat 0))]
    (do-local-insert-multi! :reference_drms drm-maps)))
  
(defn do-reference-inserts!
  [{:keys [header sequence start-codon end-codon]}]
  (try
    (let [key (-> (do-local-insert! :reference_sequence {:name header :sequence sequence :start_codon start-codon :end_codon end-codon :complete 0})
                first :generated_key)]
      (do
        (insert-reference-drms! key sequence start-codon end-codon)
        {:reference_key key}))
    (catch Exception e {:error (str (.getMessage e))})))

;; Query combiners

(defn add-genes-to-sample
  [sample-map gene-map]
  (->> (map #(assoc % :sample_id (:sample_id sample-map)) gene-map)
    (assoc sample-map :genes)))