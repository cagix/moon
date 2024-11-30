(ns forge.app.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.application :as application]
            [forge.lifecycle :as lifecycle]))

(def ^:private config "app.edn")

(defn -main []
  (let [{:keys [application lifecycle]} (-> config io/resource slurp edn/read-string)]
    (application/start application
                       (reify application/Listener
                         (create [_]
                           (lifecycle/create lifecycle))

                         (dispose [_]
                           (lifecycle/dispose))

                         (render [_]
                           (lifecycle/render))

                         (resize [_ w h]
                           (lifecycle/resize w h))))))
