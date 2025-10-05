(ns cdq.entity.image.draw
  (:require [cdq.graphics.textures :as textures]))

(defn txs
  [image
   {:keys [entity/body]}
   {:keys [ctx/graphics]}]
  [[:draw/texture-region
    (textures/texture-region graphics image)
    (:body/position body)
    {:center? true
     :rotation (or (:body/rotation-angle body)
                   0)}]])
