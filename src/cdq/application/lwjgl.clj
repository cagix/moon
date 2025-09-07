(ns cdq.application.lwjgl
  (:require [cdq.application :as application]
            [cdq.application.context :as ctx]
            [cdq.application.context.record :as ctx-record]
            [cdq.ctx]
            [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.scenes.scene2d :as scene2d]))

(extend-type cdq.application.context.record.Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (cdq.ctx/handle-draws! ctx draws)))

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
