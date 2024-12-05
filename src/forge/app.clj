(ns forge.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.java.awt :as awt]
            [clojure.lwjgl :as lwjgl]
            [forge.core :refer [app-create
                                app-dispose
                                app-render
                                app-resize]])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.utils SharedLibraryLoader)))

(defn start [{:keys [dock-icon
                     lwjgl3-config
                     components]}]
  (awt/set-dock-icon dock-icon)
  (when SharedLibraryLoader/isMac
    (lwjgl/configure {:glfw-library-name "glfw_async"
                      :glfw-check-thread0 false}))
  (lwjgl3/app (proxy [ApplicationAdapter] []
                (create []
                  (run! app-create components))

                (dispose []
                  (run! app-dispose components))

                (render []
                  (run! app-render components))

                (resize [w h]
                  (run! #(app-resize % w h) components)))
              (lwjgl3/config lwjgl3-config)))

(def exit gdx/exit)

(defmacro post-runnable [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))
