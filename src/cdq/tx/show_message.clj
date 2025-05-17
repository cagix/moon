(ns cdq.tx.show-message
  (:require [cdq.ctx :as ctx]
            [cdq.ui.message :as message]
            [gdl.ui.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn do! [message]
  (-> ctx/stage
      stage/root
      (Group/.findActor "player-message")
      (message/show! message)))
