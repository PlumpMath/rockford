(defproject rockford "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [http-kit "2.1.18"]
                 [enlive "1.1.6"]
                 [ring/ring-defaults "0.1.5"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [mysql/mysql-connector-java "5.1.37"]
                 [com.mchange/c3p0 "0.9.5.1"]
                 [honeysql "0.6.2"]
                 [net.mikera/core.matrix "0.45.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler rockford.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
