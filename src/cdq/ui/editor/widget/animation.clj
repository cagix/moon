(ns cdq.ui.editor.widget.animation
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.graphics :as graphics]
            [gdx.ui :as ui]))

(defmethod widget/create :widget/animation [_ _attribute animation {:keys [ctx/graphics]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            (ui/image-button {:texture-region (graphics/image->texture-region graphics image)
                              :scale 2}))]
   :cell-defaults {:pad 1}})
