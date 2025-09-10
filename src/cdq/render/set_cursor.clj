(ns cdq.render.set-cursor
  (:require [cdq.entity.state :as state]
            [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (graphics/set-cursor! ctx
                        (let [->cursor (state/state->cursor (:state (:entity/fsm @player-eid)))]
                          (if (keyword? ->cursor)
                            ->cursor
                            (->cursor player-eid ctx)))))
