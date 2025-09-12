(ns cdq.ui.editor.widget.edn
  (:require [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.vis-ui.text-field :as text-field]))

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text (utils/->edn-str v)
   :tooltip (str schema)})

(defn value [_  widget _schemas]
  (edn/read-string (text-field/get-text widget)))
