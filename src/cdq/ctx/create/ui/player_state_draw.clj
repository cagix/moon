(ns cdq.ctx.create.ui.player-state-draw
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(def state->draw-ui-view
  (update-vals {:player-item-on-cursor 'cdq.ctx.create.ui.player-state-draw.player-item-on-cursor/draws}
               requiring-resolve))

(defn create [_ctx]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when-let [stage (.getStage this)]
        (let [{:keys [ctx/world]
               :as ctx} (.ctx stage)
              player-eid (:world/player-eid world)
              entity @player-eid
              state-k (:state (:entity/fsm entity))]
          (when-let [f (state->draw-ui-view state-k)]
            (f player-eid ctx)))))))
