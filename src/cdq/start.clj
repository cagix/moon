(ns cdq.start
  (:require [cdq.context :as context]
            [clojure.gdx :refer [clear-screen black]]
            [clojure.utils :refer [read-edn-resource]]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag pausing? true)

(defn- render [context]
  (clear-screen black)
  (context/render context)
  (let [stage (c/stage context)]
    (ui/draw stage context)
    (ui/act  stage context))
  (context/tick context pausing?))

(defn -main []
  (let [config (read-edn-resource "app.edn")]
    (app/start (:app     config)
               (:context config)
               render)))
