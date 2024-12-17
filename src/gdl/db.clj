(ns gdl.db
  (:require [gdl.db-ctx-free :as db]))

(declare ^:private db)

(defn build [id]
  (db/build db id))

(defn build-all [property-type]
  (db/build-all db property-type))

(defn setup [config]
  (def db (db/create config)))
