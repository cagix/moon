(ns cdq.start.lwjgl
  (:require [cdq.ctx :as ctx]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [{:keys [ctx/application-state
           ctx/config]
    :as ctx}]
  (lwjgl/start-application!
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
      :resume! (fn [])})
   (:config (:cdq.start.lwjgl config))))
