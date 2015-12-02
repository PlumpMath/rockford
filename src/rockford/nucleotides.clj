(ns rockford.nucleotides)

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