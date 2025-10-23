(ns clojure.gdx.application.lwjgl
  (:require [clojure.core-ext :refer [pipeline]]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as config]
            [clojure.lwjgl.system.configuration]))

(defn start!
  [{:keys [listener
           config]}]
  ; FIXME when mac-osx
  (clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
  ; listener?
  (application/create (let [config listener
                            state @(:state config)]
                        (listener/create
                         {:create (fn []
                                    (reset! state ((:create config) com.badlogic.gdx.Gdx/app config)))

                          :dispose (fn []
                                     ((:dispose config) @state))

                          :render (fn []
                                    (swap! state pipeline (:render-pipeline config)))

                          :resize (fn [width height]
                                    ((:resize config) @state width height))

                          :pause (fn [])
                          :resume (fn [])}))
                      (config/create config)))
