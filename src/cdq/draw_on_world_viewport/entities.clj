(ns cdq.draw-on-world-viewport.entities
  (:require [cdq.graphics :as graphics]
            [cdq.raycaster :as raycaster]
            [cdq.stacktrace :as stacktrace]
            [cdq.utils :as utils]))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity [ctx entity render-layer]
  (try
   (when show-body-bounds?
     (graphics/handle-draws! ctx (draw-body-rect (:entity/body entity) (if (:body/collides? (:entity/body entity)) :white :gray))))
   ; not doseq k v but doseq render-layer-components ...
   (doseq [[k v] entity
           :let [draw-fn (get render-layer k)]
           :when draw-fn]
     (graphics/handle-draws! ctx (draw-fn v entity ctx)))
   (catch Throwable t
     (graphics/handle-draws! ctx (draw-body-rect (:entity/body entity) :red))
     (stacktrace/pretty-print t))))

(defn do!
  [{:keys [ctx/player-eid
           ctx/render-layers
           ctx/render-z-order
           ctx/active-entities
           ctx/raycaster]
    :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? raycaster player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    render-z-order)
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))
