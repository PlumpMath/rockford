(ns rockford.nucleotides
  (:require [clojure.set :as set]))

(def nuc-map 
  {\A \A,
   \B [\S \K \Y],
   \C \C,
   \D [\R \K \W],
   \G \G,
   \H [\M \Y \W],
   \K [\G \T],
   \M [\A \C],
   \R [\A \G],
   \S [\G \C],
   \T \T,
   \V [\M \S \R],
   \W [\A \T],
   \Y [\C \T]})

(def nuc-sets
 {\A #{\A},
  \B #{\C \G \T},
  \C #{\C},
  \D #{\A \G \T},
  \G #{\G},
  \H #{\A \C \T},
  \K #{\G \T},
  \M #{\A \C},
  \R #{\A \G},
  \S #{\C \G},
  \T #{\T},
  \V #{\A \C \G},
  \W #{\A \T},
  \Y #{\C \T}})

(defn get-mixed-nucs 
  "Returns a set of the base or mixture you give it
   plus any bases or mixtures below it in the hierarchy."
  [x]
  (let [nuc-value (get nuc-map x)]
    (if (= x nuc-value)
      #{x}
      (set (conj (mapcat get-mixed-nucs nuc-value) x)))))

(defn nuc-drilldown
  "Returns a set of either the nucleotide you give it
   or the nucleotides contained in the mixture you give it."
  [x]
  (let [nuc-value (get nuc-map x)]
    (if (= x nuc-value)
      #{x}
      (set (mapcat nuc-drilldown nuc-value)))))

(defn drm-assess
  [wt c r]
  (cond
    (= wt c) (if (= r wt) \. r)
    (not= wt c) (if (and (clojure.set/subset? (get nuc-sets r) (conj (get nuc-sets c) wt))
                         (not= r wt)) \. r)))

(defn split-by-seq
  "Partitions splittee (must be a vector) using the length of sequences in splitter as a template."
  [splitter splittee]
  (let [splits (reductions + 0 (map (partial apply count) splitter))]
    (map subvec (repeat (vec (map str splittee))) (butlast splits) (rest splits))))

(defn remove-inserts
  "Removes characters from codons (dash character by default) and returns a seq of vectors containing one string each."
  ([splitvec]
    (remove-inserts "-"))
  ([splitvec chars]
    (map #(vector (clojure.string/replace (reduce str %) chars "")) splitvec)))