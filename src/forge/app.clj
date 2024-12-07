(ns forge.app
  (:require [clojure.awt :as awt]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [clojure.utils :refer [defsystem install]]))

(defsystem create)
(defmethod create :default [_])

(defsystem destroy)
(defmethod destroy :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    (install "forge"
             {:optional [#'create
                         #'destroy
                         #'render
                         #'resize]}
             components)
    (awt/set-dock-icon (:dock-icon config))
    (when shared-library-loader/mac?
      (lwjgl/configure-glfw-for-mac))
    (lwjgl3/app (reify lwjgl3/Listener
                  (create  [_]     (run! create          components))
                  (dispose [_]     (run! destroy         components))
                  (render  [_]     (run! render          components))
                  (resize  [_ w h] (run! #(resize % w h) components)))
                (lwjgl3/config (:lwjgl3 config)))))
