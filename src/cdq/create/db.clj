(ns cdq.create.db
  (:require [cdq.db :as db]))

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (assoc ctx :ctx/db (db/create (:db config))))
