(ns com.badlogic.gdx.backends.lwjgl3.init.files)

(defn do! [{:keys [init/application]
            :as init}]
  (set! (.files application) (.createFiles application))
  init)
