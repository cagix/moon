(ns forge.start
  (:require [forge.base :refer :all]
            [forge.app :as app]
            [forge.db :as db]))

(defn -main []
  (let [{:keys [requires
                awt
                glfw
                lwjgl3
                db
                components]} (-> "app.edn" io-resource slurp edn-read-string)]
    (run! require requires)
    (app/set-dock-icon (:dock-icon awt))
    (app/set-glfw-config glfw)
    (db/init db)
    (app/lwjgl3-app (app/components-app components)
                    (app/lwjgl3-config lwjgl3))))
