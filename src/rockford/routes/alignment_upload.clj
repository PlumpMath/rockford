(ns rockford.routes.alignment-upload
  (:require [selmer.parser :as selmer]
            [selmer.filters :as filters]
            [rockford.routes.validators :as v]
            [rockford.db.local :as local]
            [ring.util.http-response :as response]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(filters/add-filter! :int v/parse-int)

(defn get-ref-length
  [params]
  (as-> (local/id-gets-reference {:reference-id "17"}) ref 
    (- (inc (:end_codon ref)) (:start_codon ref))))

(defn upload-page
  [{:keys [flash]}]
  (let [references (local/get-all-references)]
    (selmer/render-file "templates/alignment-upload.html" (merge {:anti-forgery (anti-forgery-field)
                                                        :refs references}
                                                                 (v/check-for-refs references)
                                                                (select-keys flash [:reference-id :alignment-name :errors])))))

(defn alignment-receiver
  [{:keys [params]}]
    (if-let [errors (v/alignment-form params)]
      (-> (response/found "/alignment/input/")
        (assoc :flash (assoc params :errors errors)))
      (let [ref-length (get-ref-length params)
           errs-then-consensus (v/get-errs-consensus params :consensus-upload :consensus-errors ref-length)
           errs-then-results (v/get-errs-results params :results-upload :results-errors ref-length)]
        (if-let [fasta-errors (merge (not-empty (first errs-then-consensus)) (not-empty (first errs-then-results)))]
          (-> (response/found "/alignment/input/")
            (assoc :flash (assoc params :errors fasta-errors)))
          (let [alignment-id (-> (local/do-local-insert! :alignment 
                                                         {:reference_id (:reference-id params) 
                                                          :alignment_name (:alignment-name params) 
                                                          :results_filename (get-in params [:results-upload :filename])})
                               first :generated_key)]
                 (do
                   (local/do-alignment-inserts! alignment-id (assoc (second errs-then-consensus) :filename (get-in params [:consensus-upload :filename])) (rest errs-then-results))
                   (response/found (str "/alignment/view/" alignment-id "/"))))))))

(defn view-all
  []
  (selmer/render-file "templates/view-all-alignments.html" {:alignments (local/get-alignments)}))

(defn view-alignment
  [aid]
  (selmer/render-file "templates/view-alignment.html" (-> (merge (first (filter #(= 3 (:id %)) (local/get-alignments)))
                                                                 (local/alignment-id-gets-consensus aid))
                                                                 (assoc :results (local/alignment-id-gets-results aid)))))
                
  ;(selmer/render-file "templates/fasta-upload-view.html" (assoc params :results (bji/parse-results-fasta-from-upload (:tempfile params)))))