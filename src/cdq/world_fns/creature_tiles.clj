(ns cdq.world-fns.creature-tiles
  (:require [cdq.graphics :as graphics]))

(defn prepare [creature-properties graphics]
  (for [{:keys [entity/animation
                creature/level
                property/id]} creature-properties
        :let [image (first (:animation/frames animation))
              texture-region (graphics/texture-region graphics image)]]
    {:creature/level level
     :tile/id id
     :tile/texture-region texture-region}))
