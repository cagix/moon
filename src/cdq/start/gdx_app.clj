(ns cdq.start.gdx-app
  (:require cdq.application
            cdq.game
            cdq.gdx.backends.lwjgl))

(defn do! [{:keys [lwjgl-app-config
                   game-create-config]}]
  (cdq.gdx.backends.lwjgl/start-application!
   lwjgl-app-config
   {:create! (fn [gdx]
               (reset! cdq.application/state (cdq.game/create! gdx game-create-config)))
    :dispose! (fn []
                (cdq.game/dispose! @cdq.application/state))
    :render! (fn []
               (swap! cdq.application/state cdq.game/render!))
    :resize! (fn [width height]
               (cdq.game/resize! @cdq.application/state width height))}))
