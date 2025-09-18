(ns cdq.ui.editor.widget.edn
  (:require [cdq.string :as string]
            [clojure.edn :as edn]))

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text (string/->edn-str v)
   :tooltip (str schema)})

(defn value [_  widget _schemas]
  (edn/read-string (:text-field/text widget)))
