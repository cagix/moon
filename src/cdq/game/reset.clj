(ns cdq.game.reset
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]))

(defn do! [world-fn]
  (utils/bind-root #'ctx/elapsed-time 0)
  (utils/bind-root #'ctx/stage ((requiring-resolve 'cdq.impl.stage/create!)))
  (utils/bind-root #'ctx/world ((requiring-resolve 'cdq.impl.world/create) ((requiring-resolve world-fn))))
  ((requiring-resolve 'cdq.game.spawn-enemies/do!))
  ((requiring-resolve 'cdq.game.spawn-player/do!)))
