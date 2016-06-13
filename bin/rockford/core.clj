(ns rockford.core
  (:require [clojure.java.io :as io]
            [rockford.db :as db]
            [clojure.string :as str]
            [clojure.tools.logging :refer :all]
            [clj-logging-config.log4j :refer :all])
  (:import [org.biojava.nbio.core.sequence.io FastaReader FastaReaderHelper GenericFastaHeaderParser DNASequenceCreator]
           [org.biojava.nbio.core.util ConcurrencyTools]
           [org.biojava.nbio.core.sequence.compound AmbiguityDNACompoundSet]
           [org.biojava.nbio.alignment Alignments Alignments$PairwiseSequenceScorerType]
           [org.biojava.nbio.alignment SimpleGapPenalty]
           [org.biojava.nbio.core.alignment.matrices SubstitutionMatrixHelper]
           [org.biojava.nbio.core.alignment.template Profile Profile$StringFormat]
           [java.io StringWriter]))

(set-loggers! "org.biojava.nbio.core.sequence.io.FastaReader" {:out (fn [ev] (println (:message ev)))})

(defn parse-int [s]
  (if (re-matches #"^\d+$" s)
    (Integer/parseInt s)
    s))

(defn dr-map->input-stream
  [xs]
  (-> (->> xs
        (map #(str ">" (:header %) "\r\n" (:sequence %)))
        (reduce #(str % "\r\n" %2)))
    (.getBytes)
    (io/input-stream)))

(defn read-fasta
  "Will accept fasta data in either input-stream or File format."
 [fasta-data]
 (let [fasta-header-parser (GenericFastaHeaderParser.)
       ambiguity-set (DNASequenceCreator. (AmbiguityDNACompoundSet/getDNACompoundSet))]
   (-> fasta-data 
     (FastaReader. fasta-header-parser ambiguity-set)
     (.process))))

(defn file->parsed-fasta
  "To read a physical fasta file."
  [file-loc]
  (-> file-loc io/input-stream read-fasta))

;(defn fasta-to-clj
;  [[k v]] 
;  (let [part-dataset (->> (str/split k #"\.") (map parse-int))]
;  {:participant_id (first part-dataset) :dataset_id (second part-dataset) :sequence (.getSequenceAsString v)}))

(defn fasta-to-clj
  [[k v]]
  {:header k :sequence (.getSequenceAsString v)})

(defn parse-result-header
  [{:keys [header sequence]}]
  (let [part-dataset (->> (str/split header #"\.") (map parse-int))]
    (if (not= 2 (count part-dataset)) (println (str "Header " header " is in the wrong format.\n"))
    {:participant_id (first part-dataset) :dataset_id (second part-dataset) :sequence sequence})))

(defn parse-fasta-from-upload
  [file-loc]
  (->> file-loc
    file->parsed-fasta
    (map fasta-to-clj)))

(defn old-parse-fasta-collect-errors
  [file-loc]
  (let [s (new java.io.StringWriter)]
    (binding [*out* s]
      (let [done-fasta (->> file-loc (file->parsed-fasta)
                         (map fasta-to-clj))]
        (cons {:fasta-errors (clojure.string/split (str s) #"\n")} done-fasta)))))

(defn parse-fasta-collect-errors
  [file-loc]
  (let [s (new java.io.StringWriter)]
    (binding [*out* s]
      (let [done-fasta (->> file-loc (file->parsed-fasta)
                         (map fasta-to-clj))]
        (if (not= "" (str s))
          (cons {:fasta-errors (clojure.string/split (str s) #"\n")} done-fasta)
          (cons {} done-fasta))))))

(defn parse-results-fasta-from-upload
  [file-loc]
  (->> file-loc
    file->parsed-fasta
    (map (comp parse-result-header fasta-to-clj))))

(defn parse-fasta-in-dataset
  [dataset-map]
  (let [parsed-fs (-> (:unparsed_fasta dataset-map)
                    (.getBytes)
                    (io/input-stream)
                    read-fasta)]
    (as-> dataset-map in-process
      (merge in-process {:header (-> parsed-fs keys first) 
        :sequence (-> parsed-fs vals first (.getSequenceAsString))})
      (assoc in-process :nucleotide_count (count (:sequence in-process))))))

(defn fasta-to-hashmap
  [fasta-data]
  (let [parsed-fs (-> fasta-data
                    (.getBytes)
                    (io/input-stream)
                    read-fasta)]
    {:header (-> parsed-fs keys first) 
     :sequence (-> parsed-fs vals first (.getSequenceAsString))}))

(defn assoc-fasta-metadata
  [fasta-db-output]
  (let [fs (first fasta-db-output)]
    (merge (dissoc fs :data) (fasta-to-hashmap (:data fs)))))

; Sequence alignment functions - not yet used

(def penalty-settings
  (doto (SimpleGapPenalty.) (.setOpenPenalty 10) (.setExtensionPenalty 1)))

(def pairwise-settings
  (Alignments$PairwiseSequenceScorerType/GLOBAL_IDENTITIES))

(def substitution-settings
  (SubstitutionMatrixHelper/getNuc4_4))

(defn do-align-write-fasta
  [parsed-fasta out-file-loc]
  (let [alignment (-> parsed-fasta .values vec 
                    (Alignments/getMultipleSequenceAlignment 
                      (into-array Object [pairwise-settings penalty-settings substitution-settings]))
                    (.toString (Profile$StringFormat/FASTA)))]
    (do
      (ConcurrencyTools/shutdown)
      (->> alignment
        (spit out-file-loc)))))

(defn do-align
  [parsed-fasta]
  (let [alignment (-> parsed-fasta .values vec 
                    (Alignments/getMultipleSequenceAlignment (into-array Object [nil]))
                   (.toString (Profile$StringFormat/FASTA)))]
    (do
      (ConcurrencyTools/shutdown)
      alignment)))