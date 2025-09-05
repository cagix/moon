(ns cdq.editor.widget.animation
  (:require [cdq.textures :as textures]
            [cdq.ui :as ui]))

(defn create [_ _attribute animation {:keys [ctx/textures]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            (ui/image-button {:texture-region (textures/image->texture-region textures image)
                              :scale 2}))]
   :cell-defaults {:pad 1}})
