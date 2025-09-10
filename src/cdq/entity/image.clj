(ns cdq.entity.image
  (:require [cdq.gdx.graphics :as graphics]))

(defn draw
  [image
   {:keys [entity/body]}
   ctx]
  [[:draw/texture-region
    (graphics/texture-region ctx image)
    (:body/position body)
    {:center? true
     :rotation (or (:body/rotation-angle body)
                   0)}]])
