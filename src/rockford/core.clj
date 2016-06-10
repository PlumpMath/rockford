(ns rockford.core
  (:require [clojure.java.io :as io]
            [rockford.db :as db]
            [clojure.string :as str])
  (:import [org.biojava.nbio.core.sequence.io FastaReader FastaReaderHelper GenericFastaHeaderParser DNASequenceCreator]
           [org.biojava.nbio.core.util ConcurrencyTools]
           [org.biojava.nbio.core.sequence.compound AmbiguityDNACompoundSet]
           [org.biojava.nbio.alignment Alignments Alignments$PairwiseSequenceScorerType]
           [org.biojava.nbio.alignment SimpleGapPenalty]
           [org.biojava.nbio.core.alignment.matrices SubstitutionMatrixHelper]
           [org.biojava.nbio.core.alignment.template Profile Profile$StringFormat]))

(defn parse-int [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))

(defn dr-map->input-stream
  [xs]
  (-> (->> xs
        (map #(str ">" (:header %) "\r\n" (:sequence %)))
        (reduce #(str % "\r\n" %2)))
    (.getBytes)
    (io/input-stream)))

(defn location->file
  "To read a physical fasta file."
  [in-file-loc]
  (io/file in-file-loc))

(defn read-fasta
  "Will accept fasta data in either input-stream or File format."
 [fasta-data]
 (let [fasta-header-parser (GenericFastaHeaderParser.)
       ambiguity-set (DNASequenceCreator. (AmbiguityDNACompoundSet/getDNACompoundSet))]
   (-> fasta-data 
     (FastaReader. fasta-header-parser ambiguity-set)
     (.process))))

(defn fasta-to-clj
  [[k v]] 
  (let [part-dataset (->> (str/split k #"\.") (map parse-int))]
  {:participant_id (first part-dataset) :dataset_id (second part-dataset) :sequence (.getSequenceAsString v)}))

(defn parse-fasta-from-upload
  [file-loc]
  (let [parsed-fs (-> file-loc io/input-stream read-fasta)]
    (map fasta-to-clj parsed-fs)))

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