(ns init.gl-emulation
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration$GLEmulation)))

(defn before-glfw
  [{:keys [init/config]
    :as init}]
  (when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (Lwjgl3Application/loadANGLE))
  init)

(defn after-window-creation
  [{:keys [init/config]
    :as init}]
  (when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (Lwjgl3Application/postLoadANGLE))
  init)
