(ns cdq.game
  (:require [cdq.application :as application]
            [cdq.application.create :as create]
            [cdq.application.dispose :as dispose]
            [cdq.application.render :as render]
            [cdq.application.resize :as resize]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx))
  (:gen-class))

(defn -main []
  (lwjgl/start-application! (reify ApplicationListener
                              (create [_]
                                (reset! application/state (create/do!
                                                           {:gdx/app      Gdx/app
                                                            :gdx/audio    Gdx/audio
                                                            :gdx/files    Gdx/files
                                                            :gdx/graphics Gdx/graphics
                                                            :gdx/input    Gdx/input})))
                              (dispose [_]
                                (dispose/do! @application/state))
                              (render [_]
                                (render/do! application/state))
                              (resize [_ width height]
                                (resize/do! @application/state width height))
                              (pause [_])
                              (resume [_]))
                            {:title "Cyber Dungeon Quest"
                             :windowed-mode {:width 1440
                                             :height 900}
                             :foreground-fps 60}
                            {:mac '[(org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
                                    (clojure.java.awt.taskbar/set-icon-image! "icon.png")]}))
