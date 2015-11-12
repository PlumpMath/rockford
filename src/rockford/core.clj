(ns rockford.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [rockford.page :as page]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]])
  (:gen-class))

(defroutes app-routes
  (GET "/" [] (page/index *anti-forgery-token*))
  (POST "/upload/" {params :params} (page/upload (get params :file-field)))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "5000"))]
    (run-server #'app {:port port})))