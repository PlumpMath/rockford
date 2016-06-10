(ns rockford.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [selmer.parser :as selmer]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.adapter.jetty :as jetty]
            [rockford.db :as db]
            [rockford.core :as bji]
            [compojure.coercions :refer :all]
            [clojure.pprint :refer [pprint]]
            [noir.response :refer [content-type status]]
            [ring.util.anti-forgery :refer [anti-forgery-field]])
  (:gen-class))

(selmer.parser/cache-off!)

(defn upload-page
  []
  (selmer/render-file "templates/alignment_upload.html" {:anti-forgery (anti-forgery-field)}))

(defn upload-trial
  [params]
  (selmer/render-file "templates/fasta_upload_view.html" (assoc params :results (bji/parse-fasta-from-upload (:tempfile params)))))
                      

(defroutes app-routes
  (GET "/alignment-upload" [] (upload-page))
  (POST "/upload_trial" request (upload-trial (:results-upload (:params request))))
  (route/resources "/")
  (route/not-found "Borfl"))

(def app
  (-> (wrap-defaults app-routes site-defaults)
    wrap-stacktrace))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "3000"))]
    (jetty/run-jetty #'app {:port port})))