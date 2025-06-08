(ns cdq.ui.editor.widget.boolean
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]))

(defmethod widget/create :boolean [_ _attribute checked? _ctx]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod widget/value :boolean [_ _attribute widget _schemas]
  (ui/checked? widget))
