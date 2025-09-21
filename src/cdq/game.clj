(ns cdq.game
  (:require [cdq.application :as application]
            [cdq.application.create :as create]
            [cdq.application.dispose :as dispose]
            [cdq.application.render :as render]
            [cdq.application.resize :as resize]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:gen-class))

(defn -main []
  (lwjgl/application {:mac '[(org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
                             (clojure.java.awt.taskbar/set-icon-image! "icon.png")]}
                     {:create (fn [gdx]
                                (reset! application/state (create/do!  gdx)))
                      :dispose (fn []
                                 (dispose/do! @application/state))
                      :render (fn []
                                (render/do! application/state))
                      :resize (fn [width height]
                                (resize/do! @application/state width height))
                      :pause (fn [])
                      :resume (fn [])}
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
