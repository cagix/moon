(ns cdq.start.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [{:keys [ctx/application-state
           ctx/config]
    :as ctx}]
  (reset! application-state ctx)
  (let [{:keys [create
                dispose
                render
                resize
                config]} (:cdq.start.lwjgl config)]
    (lwjgl/start-application!
     {:create! (fn []
                 (swap! application-state (requiring-resolve create)))
      :dispose! (fn []
                  (swap! application-state (requiring-resolve dispose)))
      :render! (fn []
                 (swap! application-state (requiring-resolve render)))
      :resize! (fn [width height]
                 (swap! application-state (requiring-resolve resize) width height))
      :pause! (fn [])
      :resume! (fn [])}
     config)))
