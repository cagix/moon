(ns com.badlogic.gdx.backends.lwjgl3.init.window)

(defn do! [{:keys [init/application
                   init/config
                   init/listener]
            :as init}]
  (assoc init :init/window (.createWindow application config listener 0)))
