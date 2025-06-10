(ns cdq.ui.editor.widget.edn
  (:require [cdq.ui.editor.widget :as widget]
            [clojure.edn :as edn]
            [gdl.ui :as ui]
            [cdq.utils :refer [->edn-str]]))

(defmethod widget/create :widget/edn [schema  _attribute v _ctx]
  {:actor/type :actor.type/text-field
   :text (->edn-str v)
   :tooltip (str schema)})

(defmethod widget/value :widget/edn [_  _attribute widget _schemas]
  (edn/read-string (ui/get-text widget)))

