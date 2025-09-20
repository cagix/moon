(ns cdq.game
  (:require [cdq.application :as application]
            [cdq.application.create]
            [cdq.application.dispose]
            [cdq.application.render]
            [cdq.application.resize]
            [clojure.config :as config]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:gen-class))

(defn -main []
  (let [create-pipeline (config/edn-resource "create.edn")
        render-pipeline (config/edn-resource "render.edn")]
    (lwjgl/start-application! {:create! (fn [gdx]
                                          (cdq.application.create/do! application/state
                                                                      gdx
                                                                      create-pipeline))
                               :dispose! (fn []
                                           (cdq.application.dispose/do! application/state))
                               :render! (fn []
                                          (cdq.application.render/do! application/state
                                                                      render-pipeline))
                               :resize! (fn [width height]
                                          (cdq.application.resize/do! application/state width height))
                               :pause! (fn [])
                               :resume! (fn [])}
                              {:title "Cyber Dungeon Quest"
                               :windowed-mode {:width 1440 :height 900}
                               :foreground-fps 60}
                              {:mac '[(org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
                                      (clojure.java.awt.taskbar/set-icon-image! "icon.png")]})))
