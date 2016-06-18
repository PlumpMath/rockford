(ns rockford.core-interop.fasta-parsing
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :refer :all]
            [clj-logging-config.log4j :refer :all])
  (:import [org.biojava.nbio.core.sequence.io FastaReader FastaReaderHelper GenericFastaHeaderParser DNASequenceCreator]
           [org.biojava.nbio.core.sequence.compound AmbiguityDNACompoundSet]
           [java.io StringWriter]))

;; Logger settings -- need to direct BioJava warnings to stdout so they can be captured

(set-loggers! "org.biojava.nbio.core.sequence.io.FastaReader" {:out (fn [ev] (println (:message ev)))})

(defn fasta-to-clj
  [[k v]]
  {:header k :sequence (.getSequenceAsString v)})

(defn replace-dashes
  [s]
  (reduce str (map #(if (= (str %) "-") \N %) s)))

(defmacro wrap-fasta-errors
 [errorname & exprs]
 `(let [s# (new java.io.StringWriter)]
    (binding [*out* s#]
      (let [fasta-output# ~@exprs
            errors# (str s#)]
        (if (not= "" errors#)
          (cons {~errorname (clojure.string/split errors# #"\n")} fasta-output#)
          (cons {} fasta-output#))))))

;; Your basic BioEdit fasta parser interop

(defn read-fasta
  "Will accept fasta data in either input-stream or File format."
 [fasta-data]
 (let [fasta-header-parser (GenericFastaHeaderParser.)
       ambiguity-set (DNASequenceCreator. (AmbiguityDNACompoundSet/getDNACompoundSet))]
   (-> fasta-data 
     (FastaReader. fasta-header-parser ambiguity-set)
     (.process))))

;; Fasta i/o

(defn file->parsed-fasta
  "To read a physical fasta file."
  [file-loc]
  (-> file-loc io/input-stream read-fasta))

;; For dealing with uploaded alignments

(defn parse-fasta-in-dataset
  [dataset-map]
  (let [parsed-fs (-> (:unparsed_fasta dataset-map)
                    (.getBytes)
                    (io/input-stream)
                    read-fasta)]
    (as-> dataset-map in-process
      (merge in-process {:header (-> parsed-fs keys first) 
        :sequence (-> parsed-fs vals first (.getSequenceAsString) replace-dashes)})
      (assoc in-process :nucleotide_count (count (:sequence in-process))))))

(defn fasta-upload-parser
  [file-loc errorname]
  (wrap-fasta-errors errorname
    (map fasta-to-clj (file->parsed-fasta file-loc))))

