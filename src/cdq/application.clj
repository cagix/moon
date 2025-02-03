(ns cdq.application
  (:require [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils :refer [operating-system]]
            [clojure.utils :refer [execute!]]))

(def state (atom nil))

(defprotocol Game
  (create [_])
  (dispose [_])
  (render [_])
  (resize [_ width height]))

(defn start [game]
  (execute! (get {:mac '[(clojure.java.awt.taskbar/set-icon "moon.png")
                         (clojure.lwjgl.system.configuration/set-glfw-library-name "glfw_async")]}
                 (operating-system)))
  (lwjgl/application (reify application/Listener
                       (create [_]
                         (reset! state (create game)))

                       (dispose [_]
                         (dispose @state))

                       (pause [_])

                       (render [_]
                         (swap! state render))

                       (resize [_ width height]
                         (resize @state width height))

                       (resume [_]))
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
