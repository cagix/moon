(ns forge.start
  (:require [clojure.gamedev :refer :all]
            [forge.app :as app]
            [forge.impl]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

#_(defprotocol Schema
    (s-explain  [_ value])
    (s-form     [_])
    (s-validate [_ data]))

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(extend-type clojure.lang.APersistentMap
  Property
  (validate! [property]
    (let [m-schema (-> property
                       schema-of-property
                       malli-form
                       m/schema)]
      (when-not (m/validate m-schema property)
        (throw (invalid-ex-info m-schema property))))))

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
