(ns cdq.draw-on-world-viewport.cell-debug
  (:require [cdq.ctx :as ctx]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]))

(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn- draw-cell-debug* [{:keys [ctx/grid
                                 ctx/factions-iterations
                                 ctx/world-viewport]}]
  (apply concat
         (for [[x y] (camera/visible-tiles (:viewport/camera world-viewport))
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

(defn do! [ctx]
  (ctx/handle-draws! ctx (draw-cell-debug* ctx)))
