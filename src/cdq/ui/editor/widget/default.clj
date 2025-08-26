(ns cdq.ui.editor.widget.default
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [truncate ->edn-str]]
            [cdq.ui.actor :as actor]))

(defmethod widget/create :default [_ _attribute v _ctx]
  {:actor/type :actor.type/label
   :label/text (truncate (->edn-str v) 60)})

(defmethod widget/value :default [_  _attribute widget _schemas]
  ((actor/user-object widget) 1))
