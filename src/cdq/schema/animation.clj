(ns cdq.schema.animation
  (:require [cdq.schema :as schema]
            [cdq.gdx.graphics :as graphics]))

(defmethod schema/malli-form :s/animation [_ schemas]
  (schema/malli-form [:s/map [:animation/frames
                              :animation/frame-duration
                              :animation/looping?]]
                     schemas))

(defn create [_ _attribute animation {:keys [ctx/graphics]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            {:actor {:actor/type :actor.type/image-button
                     :drawable/texture-region (graphics/texture-region graphics image)
                     :drawable/scale 2}})]
   :cell-defaults {:pad 1}})
