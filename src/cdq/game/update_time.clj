(ns cdq.game.update-time
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! []
  (let [delta-ms (min (graphics/delta-time) ctx/max-delta)]
    (alter-var-root #'ctx/elapsed-time + delta-ms)
    (utils/bind-root #'ctx/delta-time delta-ms)))
