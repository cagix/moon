(ns cdq.render.draw-on-world-viewport.render-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.utils :as utils]))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn do! [{:keys [ctx/active-entities
                   ctx/player-eid
                   ctx/render-z-order]
            :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (utils/sort-by-order (group-by :z-order entities)
                                                    first
                                                    render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (ctx/line-of-sight? ctx player entity))]
      (try
       (when show-body-bounds?
         (ctx/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (ctx/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (ctx/handle-draws! ctx (draw-body-rect entity :red))
         (utils/pretty-pst t))))))
