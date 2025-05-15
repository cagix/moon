(ns cdq.game.check-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :as utils]
            [clojure.input :as input]))

(def pausing? true)

(defn do! []
  (utils/bind-root #'ctx/paused? (or #_error
                                     (and pausing?
                                          (state/pause-game? (entity/state-obj @ctx/player-eid))
                                          (not (or (input/key-just-pressed? :p)
                                                   (input/key-pressed?     :space)))))))
