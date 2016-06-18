(ns rockford.routes.reference
  (:require [selmer.parser :as selmer]
            [rockford.routes.validators :as v]
            [rockford.db.local :as local]
            [rockford.core-interop.fasta-parsing :as bji]
            [ring.util.http-response :as response]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

;; Reference Fasta upload

(defn save-reference!
  [params db-response]
  (if-let [error (:error db-response)]
      (-> (response/found "/reference/input/")
            (assoc :flash (assoc-in params [:errors :fasta-errors] [error])))
      (response/found (str "/reference/input/drms/" (:reference-key db-response) "/"))))

(defn receive-reference
  "Makes sure form fields are filled; if not, redisplays with errors, otherwise proceeds to fasta validation."
  [{:keys [params]}]
  (let [int-params (merge params (v/select-ints params [:start-codon :end-codon]))
        errs-then-fasta (v/check-max-fastas params :reference-upload :fasta-errors)
        params-n-fasta (merge (second errs-then-fasta) int-params)]
    (if-let [errors (merge-with #(reduce conj %1 %2)
                                (v/reference-form int-params)
                                (v/codon-ordering params)
                                (not-empty (first errs-then-fasta))
                                (v/codons-sequence params-n-fasta))]
      (-> (response/found "/reference/input/")
       (assoc :flash (assoc int-params :errors errors)))
      (save-reference! int-params
                     (local/do-reference-inserts! params-n-fasta)))))


;; Reference DRM input

(defn choose-drms
  [{:keys [flash]} rid]
  (let [ref-name (-> (local/id-gets-reference {:reference-id rid}) first)
       drms (->> {:reference-id rid} local/reference-gets-codons (partition-all 10))]
   (selmer/render-file "templates/drms-select.html" (assoc 
                                                           {:drms drms :reference-id rid :anti-forgery (anti-forgery-field) :name (:name ref-name)}
                                                           :errors (:errors flash)))))

(defn submit-reference
  [{:keys [params]}]
  (if-let [errors (v/drm-selection params)]
    (-> (response/found (str "/reference/input/drms/" (:reference-id params) "/"))
        (assoc :flash (assoc params :errors errors)))
    (do
      (local/update-drms! params)
      (local/complete-reference params)
      (response/found (str "/reference/view/" (:reference-id params) "/")))))

;; Reference edit and delete

(defn delete-reference!
  [rid]
  (do 
    (local/delete-reference! rid)
    (response/found "/reference/view/all/")))

;; Minimal-processing Selmer renderers

(defn reference-upload
   [{:keys [flash]}]
   (selmer/render-file "templates/reference-upload.html" (merge {:anti-forgery (anti-forgery-field)} 
                                                                (select-keys flash [:start-codon :end-codon :errors]))))

(defn all-references-page
  []
  (let [references (local/get-all-references)]
    (selmer/render-file "templates/view-all-references.html" {:refs references :name (-> references first :name)})))

(defn view-reference
  [reference-id]
  (selmer/render-file "templates/view-reference.html" (merge (-> {:reference-id reference-id} local/id-gets-reference first)
                                                             {:drms (local/drms-to-text {:reference-id reference-id})})))