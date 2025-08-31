(ns cdq.ui.editor.widget.edn
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [->edn-str]]
            [clojure.edn :as edn]
            [cdq.gdx.ui :as ui]))

(defmethod widget/create :widget/edn [schema  _attribute v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text (->edn-str v)
   :tooltip (str schema)})

(defmethod widget/value :widget/edn [_  _attribute widget _schemas]
  (edn/read-string (ui/get-text widget)))

