(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.core :refer :all]))

(defn- start [{:keys [requires
                      db
                      dock-icon
                      components
                      app-config]}]
  (run! require requires)
  (db-init db)
  (set-dock-icon dock-icon)
  (when mac-os?
    (configure-glfw-for-mac-os))
  (lwjgl3-app (app-listener components)
              (lwjgl3-config app-config)))

(defn -main []
  (start (-> "app.edn"
             io/resource
             slurp
             edn/read-string)))
