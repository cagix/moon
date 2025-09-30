(ns cdq.ctx.create-db
  (:require [cdq.db :as db]))

(defn do! [ctx]
  (assoc ctx :ctx/db (db/create)))
