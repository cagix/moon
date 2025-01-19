(ns cdq.render.draw-on-world-view.before-entities
  (:require [cdq.graphics.camera :as cam]
            [cdq.graphics.shape-drawer :as sd]))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn render [{:keys [cdq.graphics/world-viewport
                      cdq.graphics/shape-drawer
                      cdq.context/factions-iterations
                      cdq.context/grid]}]
  (let [sd shape-drawer
        cam (:camera world-viewport)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (sd/grid sd
               (int left-x) (int bottom-y)
               (inc (int (:width  world-viewport)))
               (+ 2 (int (:height world-viewport)))
               1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (sd/filled-rectangle sd x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (sd/filled-rectangle sd x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (sd/filled-rectangle sd x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

