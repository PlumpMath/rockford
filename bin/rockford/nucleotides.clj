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

(defn get-nucs [x]
  (let [nuc-value (get nuc-map x)]
    (if (= x nuc-value)
      #{x}
      (set (conj (mapcat get-nucs nuc-value) x)))))