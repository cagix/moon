(ns anvil.app
  (:require [anvil.screen :as screen]
            [clojure.awt :as awt]
            [clojure.gdx.app :as app]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl :as lwjgl]
            [clojure.utils :refer [defsystem bind-root]]))

(defsystem create)
(defmethod create :default [_])

(defsystem destroy)
(defmethod destroy :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defn start [{:keys [components] :as config}]
  (awt/set-dock-icon (:dock-icon config))
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (reify lwjgl3/Listener
                (create  [_]     (run! create          components))
                (dispose [_]     (run! destroy         components))
                (render  [_]     (run! render          components))
                (resize  [_ w h] (run! #(resize % w h) components)))
              (lwjgl3/config (:lwjgl3 config))))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))

(declare screens
         current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (screen/exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root current-screen-key new-k)
    (screen/enter screen)))
