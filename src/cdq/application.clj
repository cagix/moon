(ns cdq.application
  (:require [cdq.application.context :as context]
            [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils :refer [operating-system]]
            [clojure.utils :refer [execute!]]))

(def state (atom nil))

(defn -main []
  (execute! (get {:mac '[(clojure.java.awt.taskbar/set-icon "moon.png")
                         (clojure.lwjgl.system.configuration/set-glfw-library-name "glfw_async")]}
                 (operating-system)))
  (lwjgl/application (reify application/Listener
                       (create [_]
                         (reset! state (context/create)))

                       (dispose [_]
                         (context/dispose @state))

                       (pause [_])

                       (render [_]
                         (swap! state context/render))

                       (resize [_ width height]
                         (context/resize @state width height))

                       (resume [_]))
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
