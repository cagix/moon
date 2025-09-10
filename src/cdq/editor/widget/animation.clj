(ns cdq.editor.widget.animation
  (:require [cdq.gdx.graphics :as graphics]))

(defn create [_ _attribute animation ctx]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            {:actor {:actor/type :actor.type/image-button
                     :drawable/texture-region (graphics/texture-region ctx image)
                     :drawable/scale 2}})]
   :cell-defaults {:pad 1}})
