(ns cdq.editor.widget.animation
  (:require [cdq.image :as image]
            [cdq.ui.image-button :as image-button]))

(defn create [_ _attribute animation {:keys [ctx/textures]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            (image-button/create {:texture-region (image/texture-region image textures)
                                  :scale 2}))]
   :cell-defaults {:pad 1}})
