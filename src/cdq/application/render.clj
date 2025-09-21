(ns cdq.application.render
  (:require [cdq.application :as application]
            [clojure.config :as config]
            [clojure.utils :as utils]
            [gdl.scene2d.stage :as stage]))

(def ^:private render-pipeline (config/edn-resource "render.edn"))

(defn do! []
  (swap! application/state utils/pipeline render-pipeline)
  (stage/act!  (:ctx/stage @application/state))
  (stage/draw! (:ctx/stage @application/state)))
