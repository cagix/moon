(ns cdq.ui.editor.widget.string
  (:require [cdq.ui :as ui]))

(defn create [schema  _attribute v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text v
   :tooltip (str schema)})

(defn value [_  _attribute widget _schemas]
  (ui/get-text widget))
