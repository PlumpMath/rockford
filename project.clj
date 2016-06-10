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
                 [org.biojava/biojava-core "4.2.0"]
                 [org.biojava/biojava-alignment "4.2.0"]
                 [selmer "1.0.4"]
                 [ring/ring-jetty-adapter "1.5.0-RC1"]
                 [ring/ring-devel "1.5.0-RC1"]
                 [lib-noir "0.9.9"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler rockford.handler/app
         :destroy biojava-interop.db/shutdown-datasource}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}}
  :main rockford.handler
  :aot :all)
