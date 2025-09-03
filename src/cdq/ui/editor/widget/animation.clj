(ns cdq.ui.editor.widget.animation
  (:require [cdq.graphics :as graphics]
            [cdq.gdx.ui :as ui]))

(defn create [_ _attribute animation {:keys [ctx/graphics]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            (ui/image-button {:texture-region (graphics/image->texture-region graphics image)
                              :scale 2}))]
   :cell-defaults {:pad 1}})
