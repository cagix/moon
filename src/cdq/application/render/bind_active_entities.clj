(ns cdq.application.render.bind-active-entities
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.utils :refer [bind-root]]))

(defn do! []
  (bind-root #'ctx/active-entities (content-grid/active-entities ctx/content-grid
                                                                 @ctx/player-eid)))
