(ns cdq.start.db
  (:require [cdq.db :as db]))

(defn do! [ctx params]
  (assoc ctx :ctx/db (db/create params)))
