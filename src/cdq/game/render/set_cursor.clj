(ns cdq.game.render.set-cursor
  (:require [cdq.entity.state :as state]
            [clojure.gdx :as gdx]))

(defn step
  [{:keys [ctx/gdx
           ctx/graphics
           ctx/world]
    :as ctx}]
  (let [cursors (:graphics/cursors graphics)
        eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state/cursor [state-k (state-k entity)] eid ctx)]
    (assert (contains? cursors cursor-key))
    (gdx/set-cursor! gdx (get cursors cursor-key)))
  ctx)
