(ns cdq.start.lwjgl.listener
  (:require [cdq.ctx :as ctx]))

(defn create
  [{:keys [ctx/application-state
           ctx/config]
    :as ctx}]
  (let [config (:cdq.start.lwjgl.listener config)]
    {:create! (fn []
                (reset! application-state ((requiring-resolve (:reset-game-state config))
                                           (let [ctx ((requiring-resolve (:after-gdx-create config)) ctx)]
                                             (ctx/reset-stage! ctx)
                                             ctx)
                                           (:starting-world config))))
     :dispose! (fn []
                 ((requiring-resolve (:dispose config)) @application-state))
     :render! (fn []
                (swap! application-state (requiring-resolve (:render config))))
     :resize! (fn [width height]
                ((requiring-resolve (:resize config)) @application-state width height))
     :pause! (fn [])
     :resume! (fn [])}))
