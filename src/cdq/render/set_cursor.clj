(ns cdq.render.set-cursor
  (:require [cdq.entity.state :as state]
            [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (graphics/set-cursor! graphics
                        (let [->cursor (state/state->cursor (:state (:entity/fsm @player-eid)))]
                          (if (keyword? ->cursor)
                            ->cursor
                            (->cursor player-eid ctx)))))
