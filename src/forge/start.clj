(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.core :refer :all]
            [forge.db :as db]))

(defmethods :app/db
  (app-create [[_ config]]
    (db/init config)))

(defn -main []
  (let [{:keys [requires
                awt
                glfw
                lwjgl3
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (set-dock-icon (:dock-icon awt))
    (set-glfw-config glfw)
    (lwjgl3-app (components-app components)
                (lwjgl3-config lwjgl3))))
