(ns cdq.application
  (:require cdq.application.context
            clojure.gdx.application
            clojure.gdx.backends.lwjgl
            clojure.gdx.utils
            clojure.utils))

(def state (atom nil))

(defn -main []
  (clojure.utils/execute! (get {:mac '[(clojure.java.awt.taskbar/set-icon "moon.png")
                                       (clojure.lwjgl.system.configuration/set-glfw-library-name "glfw_async")]}
                               (clojure.gdx.utils/operating-system)))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (reset! state (cdq.application.context/create)))

                                            (dispose [_]
                                              (cdq.application.context/dispose @state))

                                            (pause [_])

                                            (render [_]
                                              (swap! state cdq.application.context/render))

                                            (resize [_ width height]
                                              (cdq.application.context/resize @state width height))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60}))
