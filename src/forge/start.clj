(ns forge.start
  (:require [forge.core :refer :all]))

(defn- start [{:keys [requires
                      db
                      dock-icon
                      components
                      app-config]}]
  (run! require requires)
  (db-init db)
  (set-dock-icon dock-icon)
  (start-app [:forge.core/components-app components]
             app-config))

(defn -main []
  (start (load-edn "app.edn")))
