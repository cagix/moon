(ns cdq.render.set-cursor
  (:require [cdq.graphics :as g]
            cdq.ctx.interaction-state))

(def ^:private state->cursor
  {:active-skill :cursors/sandclock
   :player-dead :cursors/black-x
   :player-idle cdq.ctx.interaction-state/->cursor
   :player-item-on-cursor :cursors/hand-grab
   :player-moving :cursors/walking
   :stunned :cursors/denied})

(defn do! [{:keys [ctx/graphics
                   ctx/world]
            :as ctx}]
  (let [player-eid (:world/player-eid world)]
    (g/set-cursor! graphics (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                              (if (keyword? ->cursor)
                                ->cursor
                                (->cursor player-eid ctx)))))
  ctx)
