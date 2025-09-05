(ns cdq.editor.widget.animation
  (:require [cdq.textures :as textures]
            [cdq.ui.image-button :as image-button]))

(defn create [_ _attribute animation {:keys [ctx/textures]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            (image-button/create {:texture-region (textures/image->texture-region textures image)
                                  :scale 2}))]
   :cell-defaults {:pad 1}})
