(ns cdq.application.render
  (:require [clojure.config :as config]
            [clojure.utils :as utils]
            [gdl.scene2d.stage :as stage]))

(def ^:private render-pipeline (config/edn-resource "render.edn"))

(defn do! [state]
  (swap! state utils/pipeline render-pipeline)
  (stage/act!  (:ctx/stage @state))
  (stage/draw! (:ctx/stage @state)))
