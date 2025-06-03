(ns clojure.lwjgl
  (:require [clojure.application-listener :as application]
            [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.java.awt :as awt]
            [clojure.utils :as utils]))

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
    (lwjgl/start-application! (reify application/Listener
                                (create! [_]
                                  ((::create! config) config))
                                (dispose! [_]
                                  ((::dispose! config)))
                                (render! [_]
                                  ((::render! config)))
                                (resize! [_ width height]
                                  ((::resize! config) width height))
                                (pause! [_])
                                (resume! [_]))
                              (dissoc (::config config) :mac-os))))
