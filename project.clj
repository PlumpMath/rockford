(defproject rockford "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.5.0"]
                 [ring/ring-defaults "0.2.0"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [mysql/mysql-connector-java "5.1.39"]
                 [hikari-cp "1.7.1"]
                 [bouncer "1.0.0"]
                 [org.biojava/biojava-core "4.2.0" :exclusions [org.slf4j/slf4j-api
                                                                org.apache.logging.log4j/log4j-slf4j-impl]]
                 [org.biojava/biojava-alignment "4.2.0" :exclusions [org.slf4j/slf4j-api
                                                                org.apache.logging.log4j/log4j-slf4j-impl]]
                 [selmer "1.0.4"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-devel "1.4.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [lib-noir "0.9.9"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-logging-config "1.9.12"]
                 [org.slf4j/slf4j-log4j12 "1.7.14"]
                 [org.apache.logging.log4j/log4j-core "2.5"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler rockford.handler/app
         :destroy rockford.db.shutdown/shutdown-datasource}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}}
  :main rockford.handler
  :aot :all)
