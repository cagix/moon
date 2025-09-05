(ns cdq.editor.widget.edn
  (:require [cdq.utils :refer [->edn-str]]
            [clojure.edn :as edn]
            [cdq.ui.text-field :as text-field]))

(defn create [schema  _attribute v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text (->edn-str v)
   :tooltip (str schema)})

(defn value [_  _attribute widget _schemas]
  (edn/read-string (text-field/get-text widget)))

