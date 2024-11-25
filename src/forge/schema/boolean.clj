(ns ^:no-doc forge.schema.boolean
  (:require [forge.editor.widget :as widget]
            [forge.ui :as ui]))

(defmethod widget/create :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod widget/->value :boolean [_ widget]
  (com.kotcrab.vis.ui.widget.VisCheckBox/.isChecked widget))
