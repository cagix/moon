(ns cdq.game.update-time
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [cdq.world :as world])
  (:import (com.badlogic.gdx Gdx)))

(defn do! []
  (let [delta-ms (min (.getDeltaTime Gdx/graphics) world/max-delta)]
    (alter-var-root #'ctx/elapsed-time + delta-ms)
    (utils/bind-root #'ctx/delta-time delta-ms)))
