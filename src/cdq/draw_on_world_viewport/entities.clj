(ns cdq.draw-on-world-viewport.entities
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [gdl.graphics.color :as color]
            [clojure.utils :as utils]))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity
  [{:keys [ctx/graphics]
    :as ctx}
   entity render-layer]
  (try (do
        (when show-body-bounds?
          (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity)
                                                           (if (:body/collides? (:entity/body entity))
                                                             color/white
                                                             color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (graphics/handle-draws! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) color/red))
         (ctx/handle-txs! ctx
                          [[:tx/print-stacktrace t]]))))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (world/active-eids world))
        player @(:world/player-eid world)
        render-layers (:graphics/entity-render-layers graphics)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (world/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))
