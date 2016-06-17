(ns rockford.core-interop.alignment
  (:import [org.biojava.nbio.core.util ConcurrencyTools]
           [org.biojava.nbio.alignment Alignments Alignments$PairwiseSequenceScorerType]
           [org.biojava.nbio.alignment SimpleGapPenalty]
           [org.biojava.nbio.core.alignment.matrices SubstitutionMatrixHelper]
           [org.biojava.nbio.core.alignment.template Profile Profile$StringFormat]))

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