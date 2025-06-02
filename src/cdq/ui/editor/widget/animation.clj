(ns cdq.ui.editor.widget.animation
  (:require [cdq.ui.editor.widget :as widget]
            [clojure.schema :as schema]
            [clojure.ui :as ui]))

(defmethod widget/create :s/animation [_ animation ctx]
  (ui/table {:rows [(for [image (:frames animation)]
                      (ui/image-button (schema/edn->value :s/image image ctx)
                                       (fn [_actor _ctx])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))
