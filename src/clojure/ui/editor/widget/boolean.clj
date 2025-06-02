(ns clojure.ui.editor.widget.boolean
  (:require [clojure.ui.editor.widget :as widget]
            [clojure.ui :as ui]))

(defmethod widget/create :boolean [_ checked? _ctx]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod widget/value :boolean [_ widget _schemas]
  (ui/checked? widget))
