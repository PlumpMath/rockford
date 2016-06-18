(ns rockford.routes.validators
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [rockford.core-interop.fasta-parsing :as bji]
            [clojure.string :as s]))

;; General helper functions

(defn parse-int [s]
  (if (re-matches #"^\d+$" s)
    (Integer/parseInt s)
    s))

(defn select-ints
  [m keyvec]
  (->> (select-keys m keyvec)
    (reduce-kv #(assoc %1 %2 (parse-int %3)) {})))

(defn parse-header
  [fastamap]
  (let [part-dataset (->> (clojure.string/split (:header fastamap) #"\.") (map parse-int))]
    (if (= (count part-dataset) 2)
      (merge fastamap
             {:participant_id (first part-dataset) :dataset_id (second part-dataset)})
      fastamap)))

;; Predicates for bouncer validators

(defn short?
  [n]
  (fn [s]
    (< (count s) n)))

;; Bouncer validators

(v/defvalidator short-text
  {:default-message-format "Name must be less than 50 characters long"}
  [s]
  ((short? 50) s))

;; Bouncer form validation

(defn drm-selection
  "Takes map of parameters from the request, returns map of error messages."
  [params]
  (first
    (b/validate
      params
      :codon [v/required [v/every #(re-matches #"^\d+$" %)]])))

(defn reference-form
  "Takes map of parameters from the request, returns map of error messages."
  [params]
  (first
    (b/validate
      params
      [:reference-upload :filename] v/required
      :start-codon [v/required v/number]
      :end-codon [v/required v/number])))

(defn alignment-form
  [params]
  (first
    (b/validate
      params
      [:results-upload :filename] [[v/required :message "You must upload a fasta file of results"]]
      [:consensus-upload :filename] [[v/required :message "You must upload a fasta consensus sequence file"]]
      :reference-id [[v/matches #"^\d+$" :message "You must select a reference sequence"]]
      :alignment-name [[v/required :message "You must enter a name for the alignment"] short-text])))

;; Rolled my own

(defn codon-ordering
  [{:keys [end-codon start-codon]}]
  (if (and (integer? end-codon) (integer? start-codon))
    (if (<= end-codon start-codon)
      {:not-great true})))

(defn codons-sequence
  [{:keys [start-codon end-codon sequence]}]
  (if (not sequence)
    nil
    (let [codon-count (- (inc end-codon) start-codon)
          sequence-codons (/ (count sequence) 3)]
      (if (not= (- (inc end-codon) start-codon) (/ (count sequence) 3))
        {:fasta-errors [(str "Specified codon numbers do not match the sequence length. (It is " sequence-codons " codons long.)")]}
        nil))))

(defn check-for-refs
  [x]
  (if (empty? x) {:no-ref "You need to upload at least one reference sequence first!"}))

(defn file-to-fasta
  "If a file was uploaded, returns any errors plus fasta data in maps, otherwise nil."
  [params fieldname errorname]
  (if (= (:size (fieldname params)) 0)
    nil
    (bji/fasta-upload-parser (:tempfile (fieldname params)) errorname)))

(defn fasta-to-res
  [errmap]
    (cons (first errmap) (map parse-header (rest errmap))))
    
(defn check-max-fastas
 [params fieldname errorname]
 (let [fasta-err-map (file-to-fasta params fieldname errorname)]
   (if (> (count fasta-err-map) 2)
     (->> {errorname [(str "Please upload a fasta file containing no more than 1 sequence.")]}
       (merge-with #(reduce conj %1 %2) (first fasta-err-map))
       (conj (rest fasta-err-map)))
     fasta-err-map)))

(defn check-length
  [errmap len]
  (reduce #(if (not= (count (:sequence %2)) (* 3 len))
             (str % "Sequence with header " (:header %2) " is the wrong length (" (count (:sequence %2)) " bases, should be " (* 3 len) ").\n")
              %1) 
          "" (rest errmap)))

(defn check-headers
  [errmap]
  (reduce #(if (and (integer? (:participant_id %2)) (integer? (:dataset_id %2)))
             ""
             (str % "Sequence header " (:header %2) " is in the wrong format.\n")) "" (rest errmap)))

(defn get-errs-consensus
  [params fieldname errorname len]
  (let [errmap (check-max-fastas params fieldname errorname)
        lenerrs (check-length errmap len)]
    (if (empty? lenerrs)
      errmap
      (update-in errmap [1 errorname] into (s/split-lines lenerrs)))))

(defn get-errs-results
  [params fieldname errorname len]
  (let [errmap ((comp vec fasta-to-res file-to-fasta) params fieldname errorname)
        lenerrs (check-length errmap len)
        headerrs (check-headers errmap)]
    (if (empty? (str lenerrs headerrs))
      errmap
        (update-in errmap [0 errorname] concat (s/split-lines (str lenerrs headerrs))))))