(ns com.badlogic.gdx.backends.lwjgl
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defn application
  [{:keys [title
           window
           fps
           create!
           dispose!
           render!
           resize!
           colors]}]
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create!))

                        (dispose [_]
                          (dispose!))

                        (render [_]
                          (render!))

                        (resize [_ width height]
                          (resize! width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width window)
                                          (:height window))
                        (.setForegroundFPS fps))))
