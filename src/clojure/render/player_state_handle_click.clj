(ns clojure.render.player-state-handle-click
  (:require [clojure.ctx :as ctx]
            [clojure.entity :as entity]
            [clojure.state :as state]))

(defn do! [{:keys [ctx/player-eid]
            :as ctx}]
  (ctx/handle-txs! ctx
                   (state/manual-tick (entity/state-obj @player-eid)
                                      player-eid
                                      ctx))
  ctx)
