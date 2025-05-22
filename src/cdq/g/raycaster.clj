(ns cdq.g.raycaster
  (:require [cdq.g :as g]
            [cdq.raycaster :as raycaster]))

(extend-type cdq.g.Game
  g/Raycaster
  (ray-blocked? [{:keys [ctx/raycaster]} start end]
    (raycaster/blocked? raycaster
                        start
                        end))

  (path-blocked? [{:keys [ctx/raycaster]} start end width]
    (raycaster/path-blocked? raycaster
                             start
                             end
                             width)))
