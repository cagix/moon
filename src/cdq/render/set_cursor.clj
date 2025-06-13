(ns cdq.render.set-cursor
  (:require [gdl.graphics :as g]))

(defn do! [{:keys [ctx/graphics
                   ctx/player-eid]
            :as ctx}
           {:keys [state->cursor]}]
  (g/set-cursor! graphics (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                            (if (keyword? ->cursor)
                              ->cursor
                              (->cursor player-eid ctx))))
  ctx)

