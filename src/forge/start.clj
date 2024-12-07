(ns forge.start
  (:require [clojure.awt :as awt]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [clojure.utils :refer [install]]
            [forge.app :as component]))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    (install "forge"
             {:optional [#'component/create
                         #'component/destroy
                         #'component/render
                         #'component/resize]}
             components)
    (awt/set-dock-icon (:dock-icon config))
    (when shared-library-loader/mac?
      (lwjgl/configure-glfw-for-mac))
    (lwjgl3/app (reify lwjgl3/Listener
                  (create  [_]     (run! component/create          components))
                  (dispose [_]     (run! component/destroy         components))
                  (render  [_]     (run! component/render          components))
                  (resize  [_ w h] (run! #(component/resize % w h) components)))
                (lwjgl3/config (:lwjgl3 config)))))
