(ns rockford.db.shutdown
  (:require [hikari-cp.core :as hikari]
            [rockford.db.local :as local]))

(defn shutdown-datasource []
  (hikari/close-datasource local/local-datasource))