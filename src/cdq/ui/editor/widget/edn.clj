(ns cdq.ui.editor.widget.edn
  (:require [clojure.edn :as edn]
            [clojure.utils :as utils]))

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text (utils/->edn-str v)
   :tooltip (str schema)})

(defn value [_  widget _schemas]
  (edn/read-string (:text-field/text widget)))
