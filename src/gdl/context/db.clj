(ns gdl.context.db
  (:require [gdl.db :as db]))

(defn create [[_ config] _context]
  (db/create config))
