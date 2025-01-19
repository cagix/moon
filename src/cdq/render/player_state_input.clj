(ns cdq.render.player-state-input
  (:require cdq.entity.state))

(defn render [{:keys [cdq.context/player-eid] :as c}]
  (cdq.entity.state/manual-tick (cdq.entity/state-obj @player-eid) c)
  c)
