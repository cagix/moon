(ns cdq.ui.editor.widget.animation
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui]))

(defmethod widget/create :widget/animation [_ _attribute animation {:keys [ctx/graphics]}]
  (ui/table {:rows [(for [image (:animation/frames animation)]
                      (ui/image-button (graphics/image->texture-region graphics image)
                                       (fn [_actor _ctx])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))
