(ns cdq.start.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [{:keys [ctx/application-state]
    :as ctx}
   {:keys [create
           dispose
           render
           resize
           config]}]
  (reset! application-state ctx)
  (lwjgl/start-application!
   {:create! (fn [gdx]
               (swap! application-state create gdx))
    :dispose! (fn []
                (swap! application-state dispose))
    :render! (fn []
               (swap! application-state render))
    :resize! (fn [width height]
               (swap! application-state resize width height))
    :pause! (fn [])
    :resume! (fn [])}
   config))
