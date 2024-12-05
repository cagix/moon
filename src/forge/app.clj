(ns forge.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.java.awt :as awt]
            [clojure.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.utils SharedLibraryLoader)))

(defprotocol Listener
  (create [_])
  (dispose [_])
  (render [_])
  (resize [_ w h]))

(defn start [{:keys [dock-icon lwjgl3-config]} listener]
  (awt/set-dock-icon dock-icon)
  (when SharedLibraryLoader/isMac
    (lwjgl/configure {:glfw-library-name "glfw_async"
                      :glfw-check-thread0 false}))
  (lwjgl3/app (proxy [ApplicationAdapter] []
                (create []
                  (create listener))

                (dispose []
                  (dispose listener))

                (render []
                  (render listener))

                (resize [w h]
                  (resize listener w h)))
              (lwjgl3/config lwjgl3-config)))

(def exit gdx/exit)

(defmacro post-runnable [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))
