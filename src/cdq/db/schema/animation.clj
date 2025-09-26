(ns cdq.db.schema.animation
  (:require [cdq.graphics :as graphics]
            [cdq.db.schemas :as schemas]))

(defn malli-form [_ schemas]
  (schemas/create-map-schema schemas
                             [:animation/frames
                              :animation/frame-duration
                              :animation/looping?]))

(defn create-value [_ v _db]
  v)

(defn create [_ animation {:keys [ctx/graphics]}]
  {:actor/type :actor.type/table
   :rows [(for [image (:animation/frames animation)]
            {:actor {:actor/type :actor.type/image-button
                     :drawable/texture-region (graphics/texture-region graphics image)
                     :drawable/scale 2}})]
   :cell-defaults {:pad 1}})
