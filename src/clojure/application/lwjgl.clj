(ns clojure.application.lwjgl
  (:require [clojure.application :as application]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :refer [mac-osx?]]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl.system :as lwjgl-system]
            [clojure.platform.gdx]
            [clojure.utils :refer [dispose disposable? resize resizable? require-ns-resolve]])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "config.edn" io/resource slurp edn/read-string)
        render-fns (map require-ns-resolve (:render-fns config))
        create-fns (map require-ns-resolve (:create-fns config))]
    (when-let [icon (:icon config)]
      (awt/set-taskbar-icon icon))
    (when (and mac-osx?)
      (lwjgl-system/set-glfw-library-name "glfw_async"))
    (lwjgl/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                         (create []
                           (reset! state (reduce (fn [context f]
                                                   (f context config))
                                                 (gdx/context)
                                                 create-fns)))

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
                           (swap! state (fn [context]
                                          (reduce (fn [context f]
                                                    (f context))
                                                  context
                                                  render-fns))))

                         (resize [width height]
                           (doseq [[k value] @state
                                   :when (resizable? value)]
                             (when (:log-resize-lifecycle? config)
                               (println "Resizing " k " - " value))
                             (resize value width height))))
                       config)))

(defn post-runnable [f]
  (application/post-runnable (:clojure.gdx/app @state)
                             #(f @state)))
