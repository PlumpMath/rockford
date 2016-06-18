(ns rockford.routes.alignment-upload
  (:require [selmer.parser :as selmer]
            [selmer.filters :as filters]
            [rockford.routes.validators :as v]
            [rockford.db.local :as local]
            [ring.util.http-response :as response]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(filters/add-filter! :int v/parse-int)

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
      (let [errs-then-consensus (-> params (v/file-to-fasta :consensus-upload :consensus-errors) (v/check-max-fastas :fasta-errors 1))]
        errs-then-consensus)))
             
  ;(selmer/render-file "templates/fasta-upload-view.html" (assoc params :results (bji/parse-results-fasta-from-upload (:tempfile params)))))