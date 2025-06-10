(ns cdq.ui.editor.widget.enum
  (:require [cdq.ui.editor.widget :as widget]
            [clojure.edn :as edn]
            [gdl.ui :as ui]
            [cdq.utils :refer [->edn-str]]))

(defmethod widget/create :enum [schema _attribute v _ctx]
  {:actor/type :actor.type/select-box
   :items (map ->edn-str (rest schema))
   :selected (->edn-str v)})

(defmethod widget/value :enum [_  _attribute widget _schemas]
  (edn/read-string (ui/get-selected widget)))

