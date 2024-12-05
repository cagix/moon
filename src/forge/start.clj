(ns forge.start
  (:require [forge.core :refer :all]
            [forge.impl]))

(defn -main []
  (let [{:keys [requires
                dock-icon
                glfw
                lwjgl3
                db
                components]} (-> "app.edn" io-resource slurp edn-read-string)]
    (run! require requires)
    (db-init db)
    (set-dock-icon dock-icon)
    (set-glfw-config glfw)
    (start-app (components-application components)
               lwjgl3)))
