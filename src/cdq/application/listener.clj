(ns cdq.application.listener
  (:require [clojure.gdx.application.listener :as listener]))

(defn create [{:keys [state
                      create
                      dispose
                      render
                      resize]}]
  (let [state @(requiring-resolve state)]
    (listener/create
     {:create (fn []
                (reset! state (let [[f params] create]
                                ((requiring-resolve f) params))))
      :dispose (fn []
                 ((requiring-resolve dispose) @state))
      :render (fn []
                (swap! state (fn [ctx]
                               (let [[f params] render]
                                 ((requiring-resolve f) ctx params)))))
      :resize (fn [width height]
                ((requiring-resolve resize) @state width height))
      :pause (fn [])
      :resume (fn [])})))
