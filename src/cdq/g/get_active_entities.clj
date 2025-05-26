(ns cdq.g.get-active-entities
  (:require [cdq.content-grid :as content-grid]
            [cdq.g :as g]
            gdl.application))

(extend-type gdl.application.Context
  g/ActiveEntities
  (get-active-entities [{:keys [ctx/content-grid
                                ctx/player-eid]}]
    (content-grid/active-entities content-grid @player-eid)))
