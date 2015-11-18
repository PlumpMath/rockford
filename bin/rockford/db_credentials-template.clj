(ns rockford.db-credentials-template
  "Put the relevant login details in *items-spec* then rename this to db-credentials.")

(def *items-spec* {:subprotocol "mysql"
               :subname "//127.0.0.1:3306/items"
               :user "root"
               :password "xpass"})