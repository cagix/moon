(ns cdq.application.create
  (:require [clojure.config :as config]
            [clojure.utils :as utils]))

(def ^:private create-pipeline (config/edn-resource "create.edn"))

(defn do!
  [{:keys [gdx/audio
           gdx/files
           gdx/graphics
           gdx/input]}]
  (utils/pipeline
   {:ctx/audio audio
    :ctx/files files
    :ctx/graphics graphics
    :ctx/input input}
   create-pipeline))
