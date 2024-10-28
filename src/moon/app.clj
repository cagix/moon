(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [moon.component :as component]
            [moon.screen :as screen]))

(defn- module->component [module]
  (let [[ns-sym v] (if (symbol? module)
                     [module]
                     module)]
    [(keyword (str "moon." ns-sym)) v]))

(defn- require-component [[k]]
  (require (symbol k)))

(defn- load-components [components]
  (doseq [component (map module->component components) ]
    (require-component component)
    (component/on-load component)))

(defsystem create)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem resize [_ dimensions])
(defmethod resize :default [_ _])

(defn- app-listener [{:keys [components screens]}]
  (let [components (map module->component components)]
    (run! require-component components)
    (reify app/Listener
      (create [_]
        (run! create components)
        (screen/set-screens screens))

      (dispose [_]
        (run! dispose components)
        (screen/dispose-all))

      (render [_]
        (clear-screen :black)
        (screen/render (screen/current)))

      (resize [_ dimensions]
        (run! #(resize % dimensions) components)))))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (load-components (:components config))
    (app/start (:lwjgl3 config)
               (app-listener (:app config)))))
