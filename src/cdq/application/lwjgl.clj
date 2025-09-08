(ns cdq.application.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start!
  [{:keys [ctx/application-state
           ctx/lwjgl
           ctx/create-fn
           ctx/render-fn
           ctx/dispose-fn
           ctx/resize-fn]
    :as ctx}]
  (lwjgl/start-application!
   {:create! (fn []
               (reset! application-state ((requiring-resolve create-fn) ctx)))
    :dispose! (fn []
                ((requiring-resolve dispose-fn) @application-state))
    :render! (fn []
               (swap! application-state (requiring-resolve render-fn)))
    :resize! (fn [width height]
               ((requiring-resolve resize-fn) @application-state width height))
    :pause! (fn [])
    :resume! (fn [])}
   lwjgl))
