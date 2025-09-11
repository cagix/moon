(ns cdq.render.set-cursor
  (:require [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/entity-states
           ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (graphics/set-cursor! graphics
                        (let [->cursor ((:cursor entity-states) (:state (:entity/fsm @player-eid)))]
                          (if (keyword? ->cursor)
                            ->cursor
                            (->cursor player-eid ctx)))))
