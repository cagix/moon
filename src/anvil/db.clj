(ns anvil.db
  (:require [gdl.db :as db]))

(declare db)

(defn build [id]
  (db/build db id))

(defn build-all [property-type]
  (db/build-all db property-type))
