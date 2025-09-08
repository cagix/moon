(ns cdq.application.lwjgl
  (:require [cdq.application.reset-game-state]
            [cdq.application.gdx-create]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start-gdx-app
  [{:keys [ctx/application-state
           ctx/lwjgl
           ctx/render-fn
           ctx/dispose-fn
           ctx/resize-fn
           ctx/starting-world]
    :as ctx}]
  (lwjgl/start-application!
   {:create! (fn []
               (reset! application-state (cdq.application.reset-game-state/reset-game-state!
                                          (cdq.application.gdx-create/after-gdx-create! ctx)
                                          starting-world)))
    :dispose! (fn []
                ((requiring-resolve dispose-fn) @application-state))
    :render! (fn []
               (swap! application-state (requiring-resolve render-fn)))
    :resize! (fn [width height]
               ((requiring-resolve resize-fn) @application-state width height))
    :pause! (fn [])
    :resume! (fn [])}
   lwjgl))
