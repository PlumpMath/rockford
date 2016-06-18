(ns rockford.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.adapter.jetty :as jetty]
            [compojure.coercions :refer :all]
            [rockford.routes.reference :as ref]
            [rockford.routes.alignment-upload :as align-up])
  (:gen-class))

(selmer.parser/cache-off!)

(defroutes reference-routes
  (GET "/" [] ref/reference-upload)
  (context "/reference" []
            (context "/view" []
                     (GET "/:rid{[0-9]+}/" [rid :<< as-int] (ref/view-reference rid))
                     (GET "/all/" [] (ref/all-references-page)))
            (context "/input" []
                     (GET "/" [] ref/reference-upload)
                     (POST "/" [] ref/receive-reference)
                     (GET "/drms/:rid{[0-9]+}/" [rid :<< as-int params :as request] (ref/choose-drms request rid)))
            (GET "/delete/:rid{[0-9]+}/" [rid :<< as-int] (ref/delete-reference! rid))
  (POST "/submit/" [] ref/submit-reference)))

(defroutes alignment-routes
  (context "/alignment/input" []
           (GET "/" [] align-up/upload-page)
           (POST "/" [] align-up/alignment-receiver))
  (context "/alignment/view" []
           (GET "/all/" [] (align-up/view-all))
           (GET "/:aid{[0-9]+}/" [aid :<< as-int] (align-up/view-alignment aid))))
           
(defroutes last-routes
  (route/not-found "Borfl"))

(def app
  (-> (routes reference-routes alignment-routes last-routes)
    (wrap-defaults site-defaults)
    wrap-stacktrace
    wrap-reload))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "3000"))]
    (jetty/run-jetty app {:port port})))
