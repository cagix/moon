(ns moon.schema.boolean
  (:require [gdl.ui :as ui]
            [moon.schema :as schema]))

(defmethod schema/widget :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod schema/widget-value :boolean [_ widget]
  (com.kotcrab.vis.ui.widget.VisCheckBox/.isChecked widget))
