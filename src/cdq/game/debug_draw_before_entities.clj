(ns cdq.game.debug-draw-before-entities
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.world :as world]
            [clojure.graphics.camera :as camera]))

(defn do! []
  (let [cam (:camera ctx/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when ctx/show-tile-grid?
      (draw/grid (int left-x) (int bottom-y)
                 (inc (int (:width  ctx/world-viewport)))
                 (+ 2 (int (:height ctx/world-viewport)))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (world/cell ctx/world [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and ctx/show-cell-entities? (seq (:entities cell*)))
        (draw/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
        (draw/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when-let [faction ctx/show-potential-field-colors?]
        (let [{:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (ctx/factions-iterations faction))]
              (draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))
