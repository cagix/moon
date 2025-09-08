(ns cdq.start.lwjgl.listener
  (:require cdq.application.reset-game-state
            cdq.application.gdx-create))

(defn create
  [{:keys [ctx/application-state
           ctx/render-fn
           ctx/dispose-fn
           ctx/resize-fn
           ctx/starting-world]
    :as ctx}]
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
   :resume! (fn [])})
