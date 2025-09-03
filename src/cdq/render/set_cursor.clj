(ns cdq.render.set-cursor
  (:require [cdq.graphics :as graphics]))

(declare state->cursor)

(defn do!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (graphics/set-cursor! graphics (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                                   (if (keyword? ->cursor)
                                     ->cursor
                                     (->cursor player-eid ctx))))
  ctx)
