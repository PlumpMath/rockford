(ns rockford.routes.alignment-upload
  (:require [selmer.parser :as selmer]
            [selmer.filters :as filters]
            [rockford.routes.validators :as v]
            [rockford.db.local :as local]
            [rockford.core-interop.fasta-parsing :as bji]
            [ring.util.http-response :as response]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(filters/add-filter! :int bji/parse-int)

(defn upload-page
  [{:keys [params]}]
  (let [references (local/get-all-references)]
  (selmer/render-file "templates/alignment-upload.html" (merge {:anti-forgery (anti-forgery-field)
                                                      :refs references
                                                      :error (v/check-for-refs references)}
                                                              (select-keys params [:reference-id :alignment-name])))))

  (defn alignment-receiver
  [{:keys [params]}]
    (str params))
  ;(selmer/render-file "templates/fasta-upload-view.html" (assoc params :results (bji/parse-results-fasta-from-upload (:tempfile params)))))