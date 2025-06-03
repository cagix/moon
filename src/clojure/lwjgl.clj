(ns clojure.lwjgl
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.java.awt :as awt]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (when-let [mac-settings (:mac-os (::config config))]
      (when (= (gdx/operating-system) :operating-system/mac)
        (let [{:keys [glfw-async?
                      dock-icon]} mac-settings]
          (when glfw-async?
            (lwjgl/set-glfw-async!))
          (when dock-icon
            (awt/set-taskbar-icon! dock-icon)))))
    (lwjgl/start-application! (proxy [ApplicationAdapter] []
                                (create []
                                  ((::create! config) config))

                                (dispose []
                                  ((::dispose! config)))

                                (render []
                                  ((::render! config)))

                                (resize [width height]
                                  ((::resize! config) width height)))
                              (dissoc (::config config) :mac-os))))
