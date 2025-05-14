(ns cdq.game.check-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx Gdx Input$Keys)))

(def pausing? true)

(defn do! []
  (utils/bind-root #'ctx/paused? (or #_error
                                     (and pausing?
                                          (state/pause-game? (entity/state-obj @ctx/player-eid))
                                          (not (or (.isKeyJustPressed Gdx/input Input$Keys/P)
                                                   (.isKeyPressed     Gdx/input Input$Keys/SPACE)))))))
