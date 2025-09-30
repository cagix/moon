(ns cdq.application
  (:require [cdq.c :as c]
            [cdq.ctx.dispose :as dispose]
            [cdq.ctx.render :as render]
            [cdq.ctx.update-viewports :as update-viewports]
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
                         (dispose/do! @state))
                       (render [_]
                         (swap! state render/do!))
                       (resize [_ width height]
                         (update-viewports/do! @state width height))
                       (pause [_])
                       (resume [_]))
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
