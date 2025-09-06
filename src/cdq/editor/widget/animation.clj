(ns cdq.editor.widget.animation
  (:require [cdq.image :as image]))

(defn create [_ _attribute animation {:keys [ctx/textures]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            {:actor {:actor/type :actor.type/image-button
                     :drawable/texture-region (image/texture-region image textures)
                     :drawable/scale 2}})]
   :cell-defaults {:pad 1}})
