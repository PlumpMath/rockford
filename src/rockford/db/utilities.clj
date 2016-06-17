(ns rockford.db.utilities
  (:require [clojure.set :as set]))

;; General utilities

(defn rename-ids
  [xs new-id]
  (map #(set/rename-keys % {:id new-id}) xs))

(defn augment-results
  [xs keyname map-query]
  (map #(assoc % keyname (map-query %)) xs))