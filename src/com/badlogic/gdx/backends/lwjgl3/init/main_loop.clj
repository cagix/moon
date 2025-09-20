(ns com.badlogic.gdx.backends.lwjgl3.init.main-loop
  (:import (com.badlogic.gdx.utils Array)))

(defn do!
  [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application
           init/error-callback]}]
  (try
   (let [closed-windows (Array.)]
     (while (and (.running application)
                 (> (.size (.windows application)) 0))
       (.update (.audio application))
       (.loop application closed-windows)))
   (.cleanupWindows application)
   (catch Throwable t
     (throw t))
   (finally
    (org.lwjgl.glfw.GLFWErrorCallback/.free error-callback)
    (.cleanup application))))
