(ns cdq.ui.editor.widget.edn
  (:require [cdq.string :as string]
            [clojure.edn :as edn]
            [gdl.scene2d.ui.text-field :as text-field]))

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text (string/->edn-str v)
   :tooltip (str schema)})

(defn value [_  widget _schemas]
  (edn/read-string (text-field/get-text widget)))
