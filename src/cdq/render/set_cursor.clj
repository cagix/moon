(ns cdq.render.set-cursor
  (:require [cdq.ctx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/entity-states
           ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-cursor! graphics
                        (let [player-eid (:world/player-eid world)
                              ->cursor ((:cursor entity-states) (:state (:entity/fsm @player-eid)))]
                          (if (keyword? ->cursor)
                            ->cursor
                            (->cursor player-eid ctx))))
  ctx)
