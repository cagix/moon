(ns cdq.entity.player
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.utils :as utils]))

(defmethod entity/create! :entity/player? [_ eid _ctx]
  (utils/bind-root #'ctx/player-eid eid))
