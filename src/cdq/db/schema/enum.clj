(ns cdq.db.schema.enum
  (:require [clojure.edn :as edn]
            [gdl.utils :as utils]))

(defn malli-form [[_ & params] _schemas]
  (apply vector :enum params))

(defn create-value [_ v _db]
  v)

(defn create [schema v _ctx]
  {:actor/type :actor.type/select-box
   :items (map utils/->edn-str (rest schema))
   :selected (utils/->edn-str v)})

(defn value [_  widget _schemas]
  (edn/read-string (:select-box/selected widget)))
