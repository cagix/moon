(ns cdq.editor.widget.default
  (:require [cdq.utils :refer [truncate ->edn-str]]
            [cdq.ui.actor :as actor]))

(defn create [_ _attribute v _ctx]
  {:actor/type :actor.type/label
   :label/text (truncate (->edn-str v) 60)})

(defn value [_  _attribute widget _schemas]
  ((actor/user-object widget) 1))
