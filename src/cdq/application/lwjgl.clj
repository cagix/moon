(ns cdq.application.lwjgl
  (:require [cdq.application :as application]
            [cdq.application.context :as ctx]
            [cdq.application.context.record :as ctx-record]
            [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start! []
  (lwjgl/start-application!
   {:create! (fn []
               (reset! application/state (ctx/create
                                          (ctx-record/map->Context {:schema (m/schema ctx-record/schema)}))))
    :dispose! (fn []
                (ctx/dispose @application/state))
    :render! (fn []
               (swap! application/state ctx/render))
    :resize! (fn [width height]
               (ctx/resize @application/state width height))
    :pause! (fn [])
    :resume! (fn [])}
   {:title "Cyber Dungeon Quest"
    :windowed-mode {:width 1440 :height 900}
    :foreground-fps 60}))
