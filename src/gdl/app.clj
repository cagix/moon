(ns gdl.app
  (:require [clojure.gdx.app :as app]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.awt :as awt]
            [clojure.lwjgl :as lwjgl]))

(defprotocol Listener
  (create  [_])
  (dispose [_])
  (render  [_])
  (resize  [_ w h]))

(defn start [{:keys [taskbar-icon] :as config} listener]
  (when taskbar-icon
    (awt/set-taskbar-icon taskbar-icon))
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (proxy [com.badlogic.gdx.ApplicationAdapter] []
                (create  []    (create  listener))
                (dispose []    (dispose listener))
                (render  []    (render  listener))
                (resize  [w h] (resize  listener w h)))
              (lwjgl3/config config)))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))
