(ns clojure.app.gdx.lwjgl
  (:require clojure.edn
            clojure.java.io
            clojure.walk
            [gdl.application])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defn start! [config-path]
  (let [config (->> config-path
                    clojure.java.io/resource
                    slurp
                    clojure.edn/read-string
                    (clojure.walk/postwalk (fn [form]
                                             (if (symbol? form)
                                               (if (namespace form)
                                                 (requiring-resolve form)
                                                 (do
                                                  (require form)
                                                  form))
                                               form))))
        config (reify ILookup
                 (valAt [_ k]
                   (assert (contains? config k)
                           (str "Config key not found: " k))
                   (get config k)))]
    (Lwjgl3Application. (proxy [ApplicationListener] []
                          (create []
                            (let [[f params] (:gdl.application/create config)]
                              (f (gdl.application/create-context! config)
                                 params)))
                          (dispose []
                            ((:gdl.application/dispose config)))
                          (render  []
                            (let [[f params] (:gdl.application/render config)]
                              (f params)))
                          (resize [width height]
                            ((:gdl.application/resize config) width height))
                          (pause [])
                          (resume []))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:gdl.application/title config))
                          (.setWindowedMode (:width  (:gdl.application/windowed-mode config))
                                            (:height (:gdl.application/windowed-mode config)))
                          (.setForegroundFPS (:gdl.application/foreground-fps config))))))
