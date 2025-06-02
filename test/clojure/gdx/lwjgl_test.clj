(ns clojure.gdx.lwjgl-test
  (:require [clojure.lwjgl :as lwjgl]))

; TODO FIXME can't call display-mode without setting GLFW async first
; => its inutils now!

(defn -main []
  #_(let [display-mode (lwjgl/display-mode)]
      (println "display-mode: " display-mode)
      (println "primary monitor: " (lwjgl/primary-monitor))
      (lwjgl/application {:fullscreen-mode display-mode
                          :mac-os {:glfw-async? true}}))

  #_(lwjgl/application {:mac-os {:glfw-async? true}}
                     (proxy [com.badlogic.gdx.ApplicationAdapter] []))
  )
