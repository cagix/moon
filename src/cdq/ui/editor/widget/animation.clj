(ns cdq.ui.editor.widget.animation
  (:require [cdq.schema :as schema]
            [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]))

(defmethod widget/create :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (ui/image-button (schema/edn->value :s/image image)
                                       (fn on-clicked [])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))
