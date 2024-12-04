(ns forge.app
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration)))

(defn set-glfw-config [{:keys [library-name check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 check-thread0))

(defprotocol Listener
  (create [_])
  (dispose [_])
  (render [_])
  (resize [_ w h]))

(defn start [listener {:keys [title fps width height]}]
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create listener))

                        (dispose []
                          (dispose listener))

                        (render []
                          (render listener))

                        (resize [w h]
                          (resize listener w h)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))
