(ns gdl.app
  (:require [clojure.application :as application]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3.application :as lwjgl3-application]
            [clojure.gdx.extend]
            [clojure.gdx.utils.shared-library-loader :refer [mac-osx?]]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl-system-config]
            [clojure.utils :refer [dispose disposable? resize resizable?]]))

(def state (atom nil))

(defn start* [create render config]
  (when-let [icon (:icon config)]
    (awt/set-taskbar-icon icon))
  (when (and mac-osx? (:glfw-async-on-mac-osx? config))
    (lwjgl-system-config/set-glfw-library-name "glfw_async"))
  (lwjgl3-application/start (proxy [com.badlogic.gdx.ApplicationAdapter] []
                              (create []
                                (reset! state (create (gdx/context) config)))

                              (dispose []
                                ; don't dispose internal classes (:clojure.gdx/graphics,etc. )
                                ; which Lwjgl3Application will handle
                                ; otherwise app crashed w. asset-manager
                                ; which was disposed after graphics
                                ; -> so there is a certain order to cleanup...
                                (doseq [[k value] @state
                                        :when (and (not (= (namespace k) "clojure.gdx"))
                                                   (disposable? value))]
                                  (when (:log-dispose-lifecycle? config)
                                    (println "Disposing " k " - " value))
                                  (dispose value)))

                              (render []
                                (swap! state render))

                              (resize [width height]
                                (doseq [[k value] @state
                                        :when (resizable? value)]
                                  (when (:log-resize-lifecycle? config)
                                    (println "Resizing " k " - " value))
                                  (resize value width height))))
                            config))

(defn start
  ([create render]
   (start create render "config.edn"))
  ([create render edn-config]
   (start* create
           render
           (-> edn-config io/resource slurp edn/read-string))))

(defn post-runnable [f]
  (application/post-runnable (:clojure.gdx/app @state)
                             #(f @state)))
