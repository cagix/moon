(ns cdq.lwjgl
  (:require [cdq.java.awt :as awt]
            [cdq.utils :as utils]
            [clojure.application-listener :as application]
            [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]))

(defn invoke [[f params]]
  (f params))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (when (= (gdx/operating-system) :operating-system/mac)
      (let [{:keys [glfw-async?
                    dock-icon]} (:mac-os (::config config))]
        (when glfw-async?
          (lwjgl/set-glfw-async!))
        (when dock-icon
          (awt/set-taskbar-icon! dock-icon))))
    (lwjgl/start-application! (reify application/Listener
                                (create! [_]
                                  ((::create! config) config))
                                (dispose! [_]
                                  ((::dispose! config)))
                                (render! [_]
                                  (invoke (::render! config)))
                                (resize! [_ width height]
                                  ((::resize! config) width height))
                                (pause! [_])
                                (resume! [_]))
                              (dissoc (::config config) :mac-os))))
