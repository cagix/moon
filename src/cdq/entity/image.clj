(ns cdq.entity.image
  (:require [cdq.image :as image]))

(defn draw
  [image
   {:keys [entity/body]}
   {:keys [ctx/textures]}]
  [[:draw/texture-region
    (image/texture-region image textures)
    (:body/position body)
    {:center? true
     :rotation (or (:body/rotation-angle body)
                   0)}]])
