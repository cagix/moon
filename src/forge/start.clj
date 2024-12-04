(ns forge.start
  (:require [forge.base :refer :all]
            [forge.app :as app]))

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
    (app/set-glfw-config glfw)
    (app/start (reify app/Listener
                 (create [_]
                   (run! app-create components))

                 (dispose [_]
                   (run! app-dispose components))

                 (render [_]
                   (run! app-render components))

                 (resize [_ w h]
                   (run! #(app-resize % w h) components)))
               lwjgl3)))
