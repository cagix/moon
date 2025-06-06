(ns cdq.render.draw-on-world-viewport.draw-cell-debug
  (:require [cdq.grid :as grid]
            [cdq.utils.camera :as camera-utils]
            [gdl.graphics :as graphics]))

(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn- draw-cell-debug* [{:keys [ctx/grid
                                 ctx/factions-iterations
                                 ctx/graphics]}]
  (apply concat
         (for [[x y] (camera-utils/visible-tiles (:camera (:world-viewport graphics)))
               :let [cell (grid/cell grid [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance (factions-iterations faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn do! [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))
