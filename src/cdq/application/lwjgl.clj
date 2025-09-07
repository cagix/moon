(ns cdq.application.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn- render!* [ctx f]
  (let [result (if (vector? f)
                 (let [[f params] f]
                   ((requiring-resolve f) ctx params))
                 ((requiring-resolve f) ctx))]
    (if (nil? result)
      ctx
      result)))

(defn start!
  [{:keys [lwjgl-app-config
           state
           initial-record
           config
           create!
           dispose!
           render!
           resize!]}]
  (lwjgl/start-application! (let [state @(requiring-resolve state)]
                              {:create! (fn []
                                          (reset! state
                                                  (reduce render!*
                                                          (-> ((requiring-resolve initial-record))
                                                              (assoc :ctx/config config))
                                                          create!)))
                               :dispose! (fn []
                                           ((requiring-resolve dispose!) @state))
                               :render! (fn []
                                          (swap! state (fn [ctx]
                                                         (reduce render!*
                                                                 ctx
                                                                 render!))))
                               :resize! (fn [width height]
                                          ((requiring-resolve resize!) @state width height))
                               :pause! (fn [])
                               :resume! (fn [])})
                            lwjgl-app-config))
