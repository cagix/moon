(ns cdq.render.set-cursor
  (:require [cdq.entity.state :as state]
            [clojure.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/cursors
           ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (let [cursor-key (let [->cursor (state/state->cursor (:state (:entity/fsm @player-eid)))]
                     (if (keyword? ->cursor)
                       ->cursor
                       (->cursor player-eid ctx)))]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key))))
