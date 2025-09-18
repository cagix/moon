(ns com.badlogic.gdx.backends.lwjgl3.init.config
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode (.setWindowedMode object
                                     (int (:width v))
                                     (int (:height v)))
    :title (.setTitle object (str v))))

(defn- set-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :foreground-fps (.setForegroundFPS object (int v))
    (set-window-config-key! object k v)))

(defn- create-config [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (set-config-key! obj k v))
    obj))

(defn do! [{:keys [init/config
                   init/listener]
            :as init}]
  (let [config (Lwjgl3ApplicationConfiguration/copy (create-config config))]
    (when-not (.title config)
      (set! (.title config) (.getSimpleName (class listener))))
    (assoc init :init/config config)))
