(ns cdq.ui.editor.widget.boolean
  (:require [cdq.ui.editor.widget :as widget]
            [gdx.ui :as ui]))

(defmethod widget/create :boolean [_ _attribute checked? _ctx]
  (assert (boolean? checked?))
  {:actor/type :actor.type/check-box
   :text ""
   :on-clicked (fn [_])
   :checked? checked?})

(defmethod widget/value :boolean [_ _attribute widget _schemas]
  (ui/checked? widget))
