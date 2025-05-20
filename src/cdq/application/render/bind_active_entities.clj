(ns cdq.application.render.bind-active-entities
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.utils :refer [bind-root]]))

(defn do! [{:keys [ctx/content-grid
                   ctx/player-eid]}]
  (bind-root #'ctx/active-entities (content-grid/active-entities content-grid
                                                                 @player-eid)))
