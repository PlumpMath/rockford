(ns rockford.page
  (:require [net.cgrand.enlive-html :as html]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clojure.pprint :as pprint]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(html/deftemplate index-page "index.html"
  [material]
 [:div#forms] (html/html-content (:forms material))
 [:div.marketing] (html/html-content (:page-content material)))

(defn parse-csv
  [file-params]
  (with-open [in-file (io/reader (get file-params :tempfile))]
                     (doall
                       (csv/read-csv in-file))))

(defn index-forms
  [token] 
  {:forms (str "<form method=\"post\" action=\"/upload/\" name=\"submit\" enctype=\"multipart/form-data\">
   <input id=\"__anti-forgery-token\" name=\"__anti-forgery-token\" type=\"hidden\" value=\"" token "\" />
<div class=\"btn btn-default btn-file\">
			Upload raw data <input type=\"file\" name=\"file-field\">
		</div>
		<input class=\"btn btn-primary\" type=\"submit\" name=\"submit\" value=\"Submit\">
		</form>")
   :page-content "Let's see what happens."})

(defn upload-stuff
  [request]
  {:forms (apply str request) :page-content ""})

(defn index
  [token]
  (reduce str (index-page (index-forms token))))

(defn upload
  [file-params]
  (reduce str (index-page (upload-stuff (parse-csv file-params)))))