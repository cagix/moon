(ns cdq.ui.editor.widget.boolean
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui])
  (:import (com.kotcrab.vis.ui.widget VisCheckBox)))

(defmethod widget/create :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod widget/value :boolean [_ widget]
  (VisCheckBox/.isChecked widget))
