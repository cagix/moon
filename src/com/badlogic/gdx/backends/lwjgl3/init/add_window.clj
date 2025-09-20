(ns com.badlogic.gdx.backends.lwjgl3.init.add-window)

(defn do! [{:keys [init/window
                   ^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
            :as init}]
  (.add (.windows application) window)
  init)
