(ns cdq.application
  (:require [cdq.g :as g]
            [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn -main []
  (lwjgl/application {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60
                      :mac-os {:glfw-async? true
                               :dock-icon "moon.png"}}
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state ((requiring-resolve 'cdq.application.create/do!)))
                         (g/validate @state))

                       (dispose []
                         (g/validate @state)
                         ((requiring-resolve 'cdq.application.dispose/do!) @state))

                       (render []
                         (g/validate @state)
                         (swap! state (requiring-resolve 'cdq.application.render/do!))
                         (g/validate @state))

                       (resize [_width _height]
                         (g/validate @state)
                         ((requiring-resolve 'cdq.application.resize/do!) @state)))))
