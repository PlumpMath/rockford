(ns rockford.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [selmer.parser :as selmer]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.adapter.jetty :as jetty]
            [rockford.db :as db]
            [rockford.core :as bji]
            [compojure.coercions :refer :all]
            [clojure.pprint :refer [pprint]]
            [noir.response :refer [content-type status]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.http-response :as response])
  (:gen-class))

(selmer.parser/cache-off!)

(defn codon-ordering-validation
  [{:keys [end-codon start-codon]}]
  (if (and (integer? end-codon) (integer? start-codon))
    (if (<= end-codon start-codon)
      {:not-great true})))

(defn validate-reference-form
  "Takes map of parameters from the request, returns map of error messages."
  [params]
  (first
    (b/validate
      params
      [:results-upload :filename] v/required
      :start-codon [v/required v/number]
      :end-codon [v/required v/number])))

(defn file-to-fasta
  "If a file was uploaded, returns any errors plus fasta data in maps, otherwise returns empty map."
  [{:keys [results-upload]}]
  (if (= (:size results-upload) 0)
    nil
    (bji/parse-fasta-collect-errors (:tempfile results-upload))))

(defn check-max-fastas
 [fasta-err-map x]
 (if (> (count fasta-err-map) (inc x))
   (->> {:fasta-errors [(str "Please upload a fasta file containing no more than " x " sequence(s).")]}
     (merge-with #(reduce conj %1 %2) (first fasta-err-map))
     (conj (rest fasta-err-map)))
   fasta-err-map))

(defn count-reference-codons
  [{:keys [start-codon end-codon sequence]}]
  (if (not sequence)
    nil
    (let [codon-count (- (inc end-codon) start-codon)
          sequence-codons (/ (count sequence) 3)]
      (if (not= (- (inc end-codon) start-codon) (/ (count sequence) 3))
        {:fasta-errors [(str "Specified codon numbers do not match the sequence length. (It is " sequence-codons " codons long.)")]}
        nil))))

(defn save-reference!
  [params db-response]
  (if-let [error (:error db-response)]
      (-> (response/found "/")
             (assoc :flash (assoc-in params [:errors :fasta-errors] error)))
      (str "yes")))

(defn second-stage-reference-validation
  "Checks that the fasta could be parsed and saves it to the database, otherwise redisplays the page with errors."
  [params]
  (let [errs-then-fasta (-> params file-to-fasta (check-max-fastas 1))
        errs-only (if (empty? (first errs-then-fasta)) nil (first errs-then-fasta))
        int-codons (select-keys params [:start-codon :end-codon])
        codon-counter (count-reference-codons (merge int-codons (second errs-then-fasta)))]
    (if-let [errors (merge-with #(reduce conj %1 %2) (codon-ordering-validation params) errs-only codon-counter)]
      (-> (response/found "/")
        (assoc :flash (assoc params :errors errors)))
      (save-reference! params 
                     (db/do-reference-inserts! (merge (second errs-then-fasta) int-codons))))))

(defn validate-reference-for-form
  "Makes sure form fields are filled; if not, redisplays with errors, otherwise proceeds to fasta validation."
  [{:keys [params]}]
  (let [int-params (merge params
                     (reduce-kv #(assoc %1 %2 (bji/parse-int %3)) {} 
                                (select-keys params [:start-codon :end-codon])))]
    (if-let [errors (validate-reference-form int-params)]
      (-> (response/found "/")
        (assoc :flash (assoc int-params :errors errors)))
      (second-stage-reference-validation int-params))))

 (defn reference-upload-page
   [{:keys [flash]}]
   (selmer/render-file "templates/reference_upload.html" (merge {:anti-forgery (anti-forgery-field)} 
                                                                (select-keys flash [:start-codon :end-codon :errors]))))

 (defn upload-page
   []
   (selmer/render-file "templates/alignment_upload.html" {:anti-forgery (anti-forgery-field)}))

 (defn upload-trial
   [params]
   (selmer/render-file "templates/result_upload_view.html" (assoc params :results (bji/parse-results-fasta-from-upload (:tempfile params)))))
                  
 (defroutes app-routes
   (GET "/" [] reference-upload-page)
   (GET "/alignment-upload" [] (upload-page))
   (POST "/reference_upload_trial" [] validate-reference-for-form)
   (POST "/upload_trial" request (upload-trial (:results-upload (:params request))))
   (route/resources "/")
   (route/not-found "Borfl"))

 (def app
   (-> (wrap-defaults app-routes site-defaults)
     wrap-stacktrace))

 (defn -main []
   (let [port (Integer/parseInt (get (System/getenv) "PORT" "3000"))]
     (jetty/run-jetty #'app {:port port})))