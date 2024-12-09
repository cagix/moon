(ns anvil.app
  (:require [clojure.awt :as awt]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl :as lwjgl]
            [clojure.utils :refer [defsystem]]))

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
