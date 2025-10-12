(ns cdq.ui.create.player-state-draw
  (:require [cdq.graphics :as graphics]
            [clojure.gdx.scene2d.actor :as actor]))

(def state->draw-ui-view
  (update-vals {:player-item-on-cursor 'cdq.ui.create.player-state-draw.player-item-on-cursor/draws}
               requiring-resolve))

(defn- handle-draws
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [player-eid (:world/player-eid world)
        entity @player-eid
        state-k (:state (:entity/fsm entity))]
    (when-let [f (state->draw-ui-view state-k)]
      (graphics/draw! graphics (f player-eid ctx)))))

(defn create [_ctx]
  (actor/create
   {:draw (fn [this _batch _parent-alpha]
            (handle-draws (.ctx (actor/stage this))))
    :act (fn [this _delta])}))
