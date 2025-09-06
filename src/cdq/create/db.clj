(ns cdq.create.db
  (:require [cdq.db-impl :as db]))

(defn do!
  [ctx params]
  (assoc ctx :ctx/db (db/create params)))
