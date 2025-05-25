(ns cdq.start
  (:require [cdq.application :as application]
            [cdq.config :as config]))

(defn -main [config-path]
  (-> config-path
      config/create
      application/start!))

(require 'gdl.application)

(require 'cdq.game-state)
(require 'cdq.game-state.create-actors)

(require 'cdq.render)
(require 'cdq.render.render-entities)

(extend gdl.application.Context
  cdq.game-state/StageActors
  {:create-actors cdq.game-state.create-actors/create-actors}
  cdq.render/Render
  {:render-entities! cdq.render.render-entities/render-entities!})

; TODO this doesn;t work when we reload gdl.application.Context
