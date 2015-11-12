(ns rockford.db-interact
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as j]
            [clojure.core.matrix :as m]))

(def items-spec {:subprotocol "mysql"
               :subname "//127.0.0.1:3306/items"
               :user "root"
               :password "xpass"})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))

(def pooled-db-items (delay (pool items-spec)))

(defn db-connection-items [] @pooled-db-items)

(defn res-query
  [challenge-id]
  (m/array (rest 
    (j/query 
      (db-connection-items) 
      ["SELECT id, challenge_id, programme_id, participant_id, quantitative_units FROM dataset WHERE challenge_id = ?" challenge-id] 
      :as-arrays? true))))

(defn dat-query
  [sample-id]
  (m/array (rest 
    (j/query 
      (db-connection-items) 
      ["SELECT dataset_id, quantitative_value, qualitative_value, ct_value FROM result_molecular_standard WHERE challenge_sample_id = ?" sample-id] 
      :as-arrays? true))))

(defn get-chall-one
  []
  (reduce 
    #(m/join-along 1 %1 %2) 
    (res-query 1) 
    (map dat-query [1 2 3 4])))

(defn get-sample-ids
  []
  (map dat-query 
       (j/query 
         (db-connection-items) 
         ["SELECT id FROM challenge_sample LIMIT 5"] :result-set-fn #(flatten (doall %)) :row-fn vals)))

