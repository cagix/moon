(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [clojure.ctx :as ctx]
            [cdq.state :as state]
            [clojure.ui :as ui]))

(defn create [_ctx]
  (ui/actor
   {:draw (fn [_this {:keys [ctx/player-eid] :as ctx}]
            (ctx/handle-draws! ctx
                               (state/draw-gui-view (entity/state-obj @player-eid)
                                                    player-eid
                                                    ctx)))}))
