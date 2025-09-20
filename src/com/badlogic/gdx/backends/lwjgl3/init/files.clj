(ns com.badlogic.gdx.backends.lwjgl3.init.files)

(defn do! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
            :as init}]
  (set! (.files application) (.createFiles application))
  init)
