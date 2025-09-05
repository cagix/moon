(ns cdq.render.set-cursor
  (:require [clojure.gdx.graphics :as graphics]))

(declare state->cursor)

(defn do!
  [{:keys [ctx/cursors
           ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (let [cursor-key (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                     (if (keyword? ->cursor)
                       ->cursor
                       (->cursor player-eid ctx)))]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key))))
