(ns cdq.render.render-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.utils :as utils]
            [gdl.c :as c]))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn render-entities! [{:keys [ctx/active-entities
                                ctx/player-eid]
                         :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (utils/sort-by-order (group-by :z-order entities)
                                                    first
                                                    ctx/render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (g/line-of-sight? ctx player entity))]
      (try
       (when ctx/show-body-bounds?
         (c/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (c/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (c/handle-draws! ctx (draw-body-rect entity :red))
         (utils/pretty-pst t))))))
