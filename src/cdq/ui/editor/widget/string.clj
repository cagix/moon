(ns cdq.ui.editor.widget.string
  (:require [cdq.ui.editor.widget :as widget]
            [gdx.ui :as ui]))

(defmethod widget/create :string [schema  _attribute v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text v
   :tooltip (str schema)})

(defmethod widget/value :string [_  _attribute widget _schemas]
  (ui/get-text widget))
