(ns ^:no-doc forge.schema.boolean
  (:require [forge.schema :as schema]
            [gdl.ui :as ui]))

(defmethod schema/widget :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod schema/widget-value :boolean [_ widget]
  (com.kotcrab.vis.ui.widget.VisCheckBox/.isChecked widget))
