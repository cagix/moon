(ns cdq.application.render.bind-active-entities
  (:require [cdq.content-grid :as content-grid]
            [cdq.utils :refer [bind-root]]))

(defn do! [{:keys [ctx/content-grid
                   ctx/player-eid]}]
  (assoc :ctx/active-entities (content-grid/active-entities content-grid
                                                            @player-eid)))
