(ns rockford.routes.validators
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]))

;; Bouncer validators

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
      [:results-upload :filename] v/required
      [:consensus-upload :filename] v/required
      :reference-id [v/required [v/matches #"^\d+$"]]
      :alignment-name [v/required v/number])))

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

(defn check-max-fastas
 [fasta-err-map x]
 (if (> (count fasta-err-map) (inc x))
   (->> {:fasta-errors [(str "Please upload a fasta file containing no more than " x " sequence(s).")]}
     (merge-with #(reduce conj %1 %2) (first fasta-err-map))
     (conj (rest fasta-err-map)))
   fasta-err-map))

(defn check-for-refs
  [x]
  (if (empty? x) {:no-ref "You need to upload at least one reference sequence first!"}))