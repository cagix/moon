(ns cdq.schema.animation
  (:require [cdq.schemas :as schemas]
            [cdq.gdx.graphics :as graphics]))

(defn malli-form [_ schemas]
  (schemas/create-map-schema schemas
                             [:animation/frames
                              :animation/frame-duration
                              :animation/looping?]))

(defn create [_ animation {:keys [ctx/graphics]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            {:actor {:actor/type :actor.type/image-button
                     :drawable/texture-region (graphics/texture-region graphics image)
                     :drawable/scale 2}})]
   :cell-defaults {:pad 1}})
