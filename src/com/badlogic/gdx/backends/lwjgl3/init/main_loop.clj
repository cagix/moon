(ns com.badlogic.gdx.backends.lwjgl3.init.main-loop
  (:import (com.badlogic.gdx.utils Array)))

(defn do!
  [{:keys [init/application
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
    (.free error-callback)
    (.cleanup application))))
