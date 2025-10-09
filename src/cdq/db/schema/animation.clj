(ns cdq.db.schema.animation
  (:require [cdq.graphics.textures :as textures]
            [cdq.db.schemas :as schemas]
            [clojure.scene2d.build.table :as table]))

(defn malli-form [_ schemas]
  (schemas/create-map-schema schemas
                             [:animation/frames
                              :animation/frame-duration
                              :animation/looping?]))

(defn create-value [_ v _db]
  v)

(defn create [_ animation {:keys [ctx/graphics]}]
  (table/create
   {:rows [(for [image (:animation/frames animation)]
             {:actor {:actor/type :actor.type/image-button
                      :drawable/texture-region (textures/texture-region graphics image)
                      :drawable/scale 2}})]
    :cell-defaults {:pad 1}}))
