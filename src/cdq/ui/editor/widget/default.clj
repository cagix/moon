(ns cdq.ui.editor.widget.default
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]
            [cdq.utils :refer [truncate
                               ->edn-str]]))

(defmethod widget/create :default [_ _attribute v _ctx]
  {:actor/type :actor.type/label
   :text (truncate (->edn-str v) 60)})

(defmethod widget/value :default [_  _attribute widget _schemas]
  ((ui/user-object widget) 1))
