(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.app :as app]
            [forge.core :refer :all]))

(defn- start [{:keys [requires
                      db
                      dock-icon
                      components
                      app-config]}]
  (run! require requires)
  (db-init db)
  (app/start (reify app/Listener
               (create [_]
                 (run! app-create components))

               (dispose [_]
                 (run! app-dispose components))

               (render [_]
                 (run! app-render components))

               (resize [_ w h]
                 (run! #(app-resize % w h) components)))
             app-config))

(defn -main []
  (start (-> "app.edn"
             io/resource
             slurp
             edn/read-string)))
