(ns cdq.ui.player-state-draw
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn create []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (state/draw-gui-view (entity/state-obj @ctx/player-eid)))))
