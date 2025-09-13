(ns cdq.world-fns.modules.place-modules
  (:require [cdq.grid2d :as g2d]
            [cdq.world-fns.module :as module]
            [clojure.gdx.maps.tiled :as tiled]))

(defn do!
  [{:keys [scaled-grid
           grid]
    :as w}]
  (assoc w :tiled-map (module/place-module (tiled/tmx-tiled-map module/modules-file)
                                           scaled-grid
                                           grid
                                           (filter #(= :ground     (get grid %)) (g2d/posis grid))
                                           (filter #(= :transition (get grid %)) (g2d/posis grid)))))
