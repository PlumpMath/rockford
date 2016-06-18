(ns rockford.db.local
  (:require [hikari-cp.core :as hikari]
            [clojure.java.jdbc :as j]
            [rockford.db.utilities :as utils]))

;; Datasource setup

(def local-datasource-options
                         {:connection-timeout 30000
                          :idle-timeout 600000
                          :max-lifetime 1800000
                          :minimum-idle 3
                          :maximum-pool-size 3
                          :adapter "mysql"
                          :username "root"
                          :password "xpass"
                          :database-name "rockford"
                          :server-name "localhost"
                          :port-number 3306})

(def local-datasource
  (hikari/make-datasource local-datasource-options))

;; Functions to pass queries to

(defn do-local-insert!
  [table map]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/insert! conn table map)))

(defn do-local-insert-multi!
  [table maps]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/insert-multi! conn table maps)))

(defn do-local-query [query]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/query conn query)))

(defn do-local-execute! [query]
  (j/with-db-connection [conn {:datasource local-datasource}]
    (j/execute! conn query)))

(defn do-local-prepared-multi!
  [stmt & params]
  (let [prepared (into [] (cons stmt (apply map vector params)))]
    (j/with-db-connection [conn {:datasource local-datasource}]
      (j/db-do-prepared conn prepared {:multi? true}))))

;; Queries - Reference upload

(defn id-gets-reference
  [{:keys [reference-id]}]
  (-> (do-local-query ["SELECT * FROM reference where id = ? AND complete = 1" reference-id])
    (utils/rename-ids :reference_id)
    first))

(defn insert-reference-drms!
  [sequence-id sequence start-codon end-codon]
  (let [drm-maps (map #(hash-map :reference_id % :codon_id %2 :sequence %3)
                      (repeat (- (inc end-codon) start-codon) sequence-id)
                      (range start-codon (inc end-codon))
                      (map #(apply str %) (partition 3 sequence)))]
    (do-local-insert-multi! :reference_drms drm-maps)))
  
(defn do-reference-inserts!
  [{:keys [header sequence start-codon end-codon]}]
  (try
    (let [key (-> (do-local-insert! :reference {:name header :sequence sequence :start_codon start-codon :end_codon end-codon})
                first :generated_key)]
      (do
        (insert-reference-drms! key sequence start-codon end-codon)
        {:reference-key key}))
    (catch Exception e {:error (str (.getMessage e))})))

(defn reference-gets-codons
  [{:keys [reference-id]}]
  (do-local-query ["SELECT * FROM reference_drms WHERE reference_id = ?" reference-id]))

(defn update-drms!
  [{:keys [reference-id codon]}]
  (do
    (do-local-execute! ["UPDATE reference_drms SET is_drm = 0 WHERE reference_id = ?" reference-id])
    (do-local-prepared-multi! "UPDATE reference_drms SET is_drm = 1 WHERE reference_id = ? AND codon_id = ?" (repeat reference-id) codon)))

(defn complete-reference
  [{:keys [reference-id]}]
  (do-local-execute! ["UPDATE reference SET complete = 1 WHERE id = ?" reference-id]))

;; Reference view

(defn get-all-references
  []
  (let [results (do-local-query (str "SELECT d.reference_id, r.reference_name, r.start_codon, r.end_codon, COUNT(*) 'number_of_drms' FROM reference r INNER JOIN reference_drms "
                                           "d ON r.id = d.reference_id WHERE r.complete = 1 AND d.is_drm = 1 GROUP BY d.reference_id"))]
    (map #(assoc % :length (- (inc (:end_codon %)) (:start_codon %))) results)))

;; Reference edit and delete

(defn delete-reference!
  [rid]
  (do-local-execute! ["UPDATE reference SET complete = 0 WHERE id = ?" rid]))

;; Alignment upload

(defn do-alignment-inserts!
  [alignment-id consensus results]
  (do
    (do-local-insert! :consensus (assoc consensus :alignment_id alignment-id))
    (do-local-insert-multi! :alignment_results (map #(-> % (assoc :alignment_id alignment-id) (dissoc :header)) results))
    (do-local-execute! ["UPDATE alignment SET complete = 1 WHERE id = ?" alignment-id])))

;; Alignment view

(defn get-alignments
  []
  (do-local-query [(str "SELECT a.id, a.alignment_name, a.results_filename, r.reference_name, COUNT(*) 'number_results' FROM alignment a INNER JOIN reference r "
                        "ON a.reference_id = r.id INNER JOIN alignment_results ar ON a.id = ar.alignment_id GROUP BY a.id")]))

(defn alignment-id-gets-consensus
  [aid]
  (first (do-local-query ["SELECT * FROM consensus WHERE alignment_id = ?" aid])))

(defn alignment-id-gets-results
  [aid]
  (do-local-query ["SELECT * FROM alignment_results WHERE alignment_id = ?" aid]))

;; Query filters

(defn reference-gets-drms
  [x]
  (filter #(true? (:is_drm %)) (reference-gets-codons x)))

(defn drms-to-text
  [x]
  (let [codons-only (map :codon_id (reference-gets-drms x))]
  (reduce #(str % ", Codon " %2) (str "Codon "(first codons-only)) (rest codons-only))))

(defn return-reference-details
  []
  (-> (get-all-references) (utils/rename-ids :reference-id) (utils/augment-results :drms drms-to-text)))