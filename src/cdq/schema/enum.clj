(ns cdq.schema.enum
  (:require [cdq.string :as string]
            [clojure.edn :as edn]
            [com.kotcrab.vis.ui.widget.select-box :as select-box]))

(defn malli-form [[_ & params] _schemas]
  (apply vector :enum params))

(defn create-value [_ v _db]
  v)

(defn create [schema v _ctx]
  {:actor/type :actor.type/select-box
   :items (map string/->edn-str (rest schema))
   :selected (string/->edn-str v)})

(defn value [_  widget _schemas]
  (edn/read-string (select-box/get-selected widget)))
