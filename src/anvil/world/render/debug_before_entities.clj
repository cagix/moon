(ns anvil.world.render.debug-before-entities
  (:require [anvil.world :as world]
            [anvil.world.render :as render]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn-impl render/debug-before-entities [c]
  (let [cam c/camera
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (c/grid c
              (int left-x) (int bottom-y)
              (inc (int c/world-viewport-width))
              (+ 2 (int c/world-viewport-height))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (world/grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (c/filled-rectangle c x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (c/filled-rectangle c x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (world/factions-iterations faction))]
              (c/filled-rectangle c x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))
