(ns cdq.game.debug-draw-before-entities
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn do! []
  (let [cam (:camera ctx/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/draw-grid (int left-x) (int bottom-y)
                          (inc (int (:width  ctx/world-viewport)))
                          (+ 2 (int (:height ctx/world-viewport)))
                          1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell ((:grid ctx/world) [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/draw-filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/draw-filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/draw-filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))
