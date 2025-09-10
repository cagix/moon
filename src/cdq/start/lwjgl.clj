(ns cdq.start.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [{:keys [ctx/application-state
           ctx/config]
    :as ctx}]
  (lwjgl/start-application!
   (let [{:keys [create
                 dispose
                 render
                 resize]
          :as config} (:cdq.start.lwjgl.listener config)]
     {:create! (fn []
                 (reset! application-state ((requiring-resolve create) ctx)))
      :dispose! (fn []
                  ((requiring-resolve dispose) @application-state))
      :render! (fn []
                 (swap! application-state (requiring-resolve render)))
      :resize! (fn [width height]
                 ((requiring-resolve resize) @application-state width height))
      :pause! (fn [])
      :resume! (fn [])})
   (:config (:cdq.start.lwjgl config))))
