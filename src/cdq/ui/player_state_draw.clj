(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [clojure.gdx.ui :as ui]
            [gdl.c :as c]))

(defn create [_ctx]
  (ui/actor
   {:draw (fn [_this {:keys [ctx/player-eid] :as ctx}]
            (c/handle-draws! ctx
                             (state/draw-gui-view (entity/state-obj @player-eid)
                                                  player-eid
                                                  ctx)))}))
