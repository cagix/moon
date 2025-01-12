(ns gdl.app.desktop
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.platform.libgdx]
            [gdl.utils :refer [dispose disposable? resize resizable? require-ns-resolve]])
  (:import (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration))
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [{:keys [title
                fps
                width
                height]
         :as config} (-> "config.edn" io/resource slurp edn/read-string)
        render-fns (map require-ns-resolve (:render-fns config))
        create-fns (:create-fns config)]
    (when-let [icon (:icon config)]
      (awt/set-taskbar-icon icon))
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (lwjgl3/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                          (create []
                            (reset! state (reduce (fn [context [k [var params]]]
                                                    (println (keys context))
                                                    (let [f (require-ns-resolve var)]
                                                      (assert f (str var))
                                                      (assoc context k (f context params))))
                                                  (assoc (into {}
                                                               (for [[k v] (gdx/context)]
                                                                 [(keyword (str "gdl/" (name k))) v]))
                                                         :gdl/config config)
                                                  create-fns)))

                          (dispose []
                            ; don't dispose internal classes (:gdl/graphics,etc. )
                            ; which Lwjgl3Application will handle
                            ; otherwise app crashed w. asset-manager
                            ; which was disposed after graphics
                            ; -> so there is a certain order to cleanup...
                            (doseq [[k value] @state
                                    :when (and (not (= (namespace k) "gdl"))
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
                        (lwjgl3/config (:lwjgl3 config)))))

(defn post-runnable [f]
  (app/post-runnable (:gdl/app @state)
                     #(f @state)))
