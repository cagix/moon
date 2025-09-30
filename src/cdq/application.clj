(ns cdq.application
  (:require [cdq.c :as c]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener))
  (:gen-class))

(def state (atom nil))

(defn -main []
  (lwjgl-system/set-glfw-library-name! "glfw_async")
  (lwjgl/application (reify ApplicationListener
                       (create [_]
                         (reset! state (c/create! {:ctx/audio    (gdx/audio)
                                                   :ctx/files    (gdx/files)
                                                   :ctx/graphics (gdx/graphics)
                                                   :ctx/input    (gdx/input)})))
                       (dispose [_]
                         (c/dispose! @state))
                       (render [_]
                         (swap! state c/render!))
                       (resize [_ width height]
                         (c/resize! @state width height))
                       (pause [_])
                       (resume [_]))
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
